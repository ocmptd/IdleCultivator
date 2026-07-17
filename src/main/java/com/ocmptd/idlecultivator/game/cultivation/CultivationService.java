package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * 双轨制修炼系统:
 * - 快速修炼(≤30 分钟):固定 +100 修为 +50 灵石,到期自动结算并推送,无溢出风险
 * - 长时修炼(30 分钟 ~ 24 小时):时长 × 基础收益,到期后需 !收获;超时溢出部分按分级惩罚转化:
 *   ≤1h → 灵尘(×0.3);1-3h → 残破法宝(×0.5);>3h → 灵石(×0.7)且 20% 概率走火入魔(修为 -10%)
 */
public class CultivationService {
    private static final Logger log = LoggerFactory.getLogger(CultivationService.class);

    public static final int DEFAULT_MINUTES = 30;
    public static final long QUICK_EXP = 100;
    public static final long QUICK_STONES = 50;
    /** 长时修炼每小时基础修为(4h ≈ 250) */
    public static final double BASE_EXP_PER_HOUR = 62.5;
    public static final String ITEM_DUST = "灵尘";
    public static final String ITEM_BROKEN_ARTIFACT = "残破法宝";
    public static final double MADNESS_PROBABILITY = 0.2;

    private final CultivationTaskRepository taskRepository;
    private final PlayerService playerService;
    /** 修炼结算/到期提醒的推送回调:(task, 文本) */
    private volatile BiConsumer<CultivationTask, String> notifier = (t, msg) -> log.info("[推送] {}: {}", t.userId(), msg);

    public CultivationService(CultivationTaskRepository taskRepository, PlayerService playerService) {
        this.taskRepository = taskRepository;
        this.playerService = playerService;
    }

    public void setNotifier(BiConsumer<CultivationTask, String> notifier) {
        this.notifier = notifier;
    }

    public Optional<CultivationTask> activeTask(String userId) {
        return taskRepository.findActiveByUser(userId);
    }

    /**
     * 开始修炼。
     *
     * @param minutes 修炼时长(分钟),默认 30
     */
    public String start(Player player, String method, int minutes) {
        Optional<CultivationTask> active = taskRepository.findActiveByUser(player.userId());
        if (active.isPresent()) {
            if (active.get().status() == CultivationTask.STATUS_READY) {
                return "道友上次修炼已完成,请先收获(收获指令)再开始新的修炼。";
            }
            return "道友已在修炼中,请先等待本次修炼完成。";
        }
        if (minutes <= 0) minutes = DEFAULT_MINUTES;
        if (minutes > 24 * 60) return "单次修炼时长不可超过 24 小时。";
        long reward = expectedReward(minutes);
        taskRepository.insert(player.userId(), player.groupId(), method, System.currentTimeMillis(), minutes, reward);
        String methodDesc = method == null ? "" : "《" + method + "》";
        String tail = isQuick(minutes) ? ",完成后自动收获" : ",完成后请及时收获,超时将有溢出惩罚";
        return "道友开始修炼" + methodDesc + ",预计 " + formatDuration(minutes) + " 后完成,将获得 +" + reward + " 修为" + tail;
    }

    public static boolean isQuick(int minutes) {
        return minutes <= DEFAULT_MINUTES;
    }

    public long expectedReward(int minutes) {
        if (isQuick(minutes)) return QUICK_EXP;
        return Math.round(minutes / 60.0 * BASE_EXP_PER_HOUR);
    }

    /**
     * 到期任务处理,由调度器周期调用:快速修炼自动结算;长时修炼标记待收获并提醒一次。
     */
    public void settleFinishedTasks() {
        List<CultivationTask> finished = taskRepository.findFinishedRunning(System.currentTimeMillis());
        for (CultivationTask task : finished) {
            try {
                if (isQuick(task.durationMinutes())) {
                    settleQuick(task);
                } else {
                    taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_READY);
                    notifier.accept(task, "道友修炼已满 " + formatDuration(task.durationMinutes())
                            + ",可以收获了!超时将按溢出惩罚转化收益。");
                }
            } catch (Exception e) {
                log.error("处理修炼任务 {} 失败", task.taskId(), e);
            }
        }
    }

    private void settleQuick(CultivationTask task) {
        Optional<Player> opt = playerService.find(task.userId());
        if (opt.isEmpty()) {
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_EXPIRED);
            return;
        }
        Player player = opt.get();
        player.addExp(task.expectedReward());
        player.addSpiritStones(QUICK_STONES);
        playerService.save(player);
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        notifier.accept(task, "道友修炼完成!获得 +" + task.expectedReward() + " 修为,+" + QUICK_STONES
                + " 灵石,当前修为:" + player.exp() + "/" + player.realm().expToNext());
    }

    /**
     * 收获长时修炼(!收获)。快速修炼自动结算无需调用。
     */
    public String harvest(Player player) {
        Optional<CultivationTask> opt = taskRepository.findActiveByUser(player.userId());
        if (opt.isEmpty()) return "道友当前没有可收获的修炼。";
        CultivationTask task = opt.get();
        long now = System.currentTimeMillis();
        if (now < task.endTime()) {
            long remain = (task.endTime() - now) / 60_000 + 1;
            return "修炼尚未完成,还需约 " + remain + " 分钟。";
        }
        if (isQuick(task.durationMinutes())) {
            settleQuick(task);
            return "道友修炼完成!收益已自动结算,当前修为:" + playerService.find(player.userId()).map(Player::exp).orElse(0L)
                    + "/" + player.realm().expToNext();
        }

        long overtimeMinutes = (now - task.endTime()) / 60_000;
        long baseExp = task.expectedReward();
        StringBuilder msg = new StringBuilder();
        if (overtimeMinutes < 1) {
            player.addExp(baseExp);
            msg.append("道友修炼完成!获得 +").append(baseExp).append(" 修为");
        } else {
            long overflowExp = Math.min(baseExp, Math.round(overtimeMinutes / 60.0 * BASE_EXP_PER_HOUR));
            long gainExp = baseExp - overflowExp;
            player.addExp(gainExp);
            msg.append("道友修炼完成!获得 +").append(gainExp).append(" 修为(超时 ")
                    .append(formatDuration((int) overtimeMinutes)).append(",溢出部分已转化:");
            if (overtimeMinutes <= 60) {
                int dust = (int) Math.max(1, Math.round(overflowExp * 0.3));
                player.setInventory(Inventory.add(player.inventory(), ITEM_DUST, dust));
                msg.append(ITEM_DUST).append("×").append(dust).append(")");
            } else if (overtimeMinutes <= 180) {
                int artifacts = (int) Math.max(1, Math.round(overflowExp * 0.5 / 50));
                player.setInventory(Inventory.add(player.inventory(), ITEM_BROKEN_ARTIFACT, artifacts));
                msg.append(ITEM_BROKEN_ARTIFACT).append("×").append(artifacts).append(")");
            } else {
                long stones = Math.max(1, Math.round(overflowExp * 0.7));
                player.addSpiritStones(stones);
                msg.append("灵石×").append(stones).append(")");
                if (ThreadLocalRandom.current().nextDouble() < MADNESS_PROBABILITY) {
                    long loss = Math.round(player.exp() * 0.1);
                    player.setExp(Math.max(0, player.exp() - loss));
                    msg.append("\n不好!道友走火入魔,修为受损 -").append(loss).append("!");
                }
            }
        }
        playerService.save(player);
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        msg.append("\n当前修为:").append(player.exp()).append("/").append(player.realm().expToNext());
        return msg.toString();
    }

    public static String formatDuration(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) return (minutes / 60) + " 小时";
        if (minutes > 60) return (minutes / 60) + " 小时 " + (minutes % 60) + " 分钟";
        return minutes + " 分钟";
    }
}

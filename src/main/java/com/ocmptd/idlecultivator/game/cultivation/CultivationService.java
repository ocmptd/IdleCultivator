package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.LevelTable;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 双轨制修炼系统:
 * - 快速修炼(≤30 分钟):固定 +100 修为 +50 灵石,到期自动结算并推送,默认时长 18~28 分钟随机
 * - 长时修炼(30 分钟 ~ 24 小时):时长 × 基础收益 × 功法倍率,到期后需收获(推送失败时自动结算);
 *   超时溢出部分按分级惩罚转化:≤1h → 灵尘(×0.3);1-3h → 残破法宝(×0.5);>3h → 灵石(×0.7)且 20% 概率走火入魔
 * - 提前收获:修炼满 10 分钟后可提前结束,收益 = 预期 × 进度 × (0.5 + 0.5×进度),越接近完成衰减越小
 */
public class CultivationService {
    private static final Logger log = LoggerFactory.getLogger(CultivationService.class);

    /** 修炼结算/到期提醒的推送回调,返回是否推送成功 */
    @FunctionalInterface
    public interface Notifier {
        boolean push(CultivationTask task, String message);
    }

    public static final int DEFAULT_MINUTES = 30;
    public static final int MIN_HARVEST_MINUTES = 10;
    public static final long QUICK_EXP = 100;
    public static final long QUICK_STONES = 50;
    /** 长时修炼每小时基础修为(4h ≈ 250) */
    public static final double BASE_EXP_PER_HOUR = 62.5;
    public static final String ITEM_DUST = "灵尘";
    public static final String ITEM_BROKEN_ARTIFACT = "残破法宝";
    public static final double MADNESS_PROBABILITY = 0.2;

    private final CultivationTaskRepository taskRepository;
    private final PlayerService playerService;
    private volatile Notifier notifier = (t, msg) -> {
        log.info("[推送] {}: {}", t.userId(), msg);
        return false;
    };

    public CultivationService(CultivationTaskRepository taskRepository, PlayerService playerService) {
        this.taskRepository = taskRepository;
        this.playerService = playerService;
    }

    public void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public Optional<CultivationTask> activeTask(String userId) {
        return taskRepository.findActiveByUser(userId);
    }

    /** 默认修炼时长:18~28 分钟随机 */
    public static int randomDefaultMinutes() {
        return ThreadLocalRandom.current().nextInt(18, 29);
    }

    /**
     * 开始修炼。
     *
     * @param method  功法,null 表示默认吐纳诀
     * @param minutes 修炼时长(分钟),≤0 表示默认 18~28 分钟随机
     */
    public String start(Player player, CultivationMethod method, int minutes) {
        Optional<CultivationTask> active = taskRepository.findActiveByUser(player.userId());
        if (active.isPresent()) {
            CultivationTask t = active.get();
            if (t.status() == CultivationTask.STATUS_READY || t.endTime() <= System.currentTimeMillis()) {
                return "道友上次修炼已完成,请先收获再开始新的修炼。";
            }
            long remain = Math.max(1, (t.endTime() - System.currentTimeMillis()) / 60_000 + 1);
            return "道友已在修炼中,剩余约 " + remain + " 分钟(满 " + MIN_HARVEST_MINUTES + " 分钟后可提前收获)。";
        }
        if (method == null) method = CultivationMethod.TU_NA;
        if (player.realm().ordinal() < method.requiredRealm().ordinal()) {
            return "《" + method.label() + "》需" + method.requiredRealm().label() + "及以上方可修炼。";
        }
        if (minutes <= 0) minutes = randomDefaultMinutes();
        if (minutes > 24 * 60) return "单次修炼时长不可超过 24 小时。";
        long reward = expectedReward(minutes, method, player.level());
        taskRepository.insert(player.userId(), player.groupId(), method.label(), System.currentTimeMillis(), minutes, reward);
        String tail = isQuick(minutes) ? ",完成后自动收获" : ",完成后请及时收获,超时将有溢出惩罚";
        return "道友开始修炼《" + method.label() + "》,预计 " + formatDuration(minutes) + " 后完成,将获得 +" + reward + " 修为" + tail;
    }

    public static boolean isQuick(int minutes) {
        return minutes <= DEFAULT_MINUTES;
    }

    public long expectedReward(int minutes, CultivationMethod method) {
        return expectedReward(minutes, method, 1);
    }

    /** 预期收益 = 基础 × 功法倍率 × 境界压制(元婴期 ×0.8)。 */
    public long expectedReward(int minutes, CultivationMethod method, int level) {
        double multiplier = (method == null ? 1.0 : method.multiplier()) * LevelTable.cultivationMultiplier(level);
        if (isQuick(minutes)) return Math.round(QUICK_EXP * multiplier);
        return Math.round(minutes / 60.0 * BASE_EXP_PER_HOUR * multiplier);
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
                    boolean pushed = notifier.push(task, "道友修炼已满 " + formatDuration(task.durationMinutes())
                            + ",可以收获了!超时将按溢出惩罚转化收益。");
                    if (pushed) {
                        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_READY);
                    } else {
                        settleLongOnTime(task);
                    }
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
        player.addSpiritStones(QUICK_STONES);
        String levelTip = playerService.gainExp(player, task.expectedReward());
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        notifier.push(task, "道友修炼完成!获得 +" + task.expectedReward() + " 修为,+" + QUICK_STONES
                + " 灵石," + progressOf(player) + levelTip);
    }

    /** 无法推送提醒时,长时修炼到期直接自动结算全额收益,避免玩家被溢出惩罚。 */
    private void settleLongOnTime(CultivationTask task) {
        Optional<Player> opt = playerService.find(task.userId());
        if (opt.isEmpty()) {
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_EXPIRED);
            return;
        }
        Player player = opt.get();
        playerService.gainExp(player, task.expectedReward());
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        log.info("推送不可用,长时修炼自动结算:user={} +{} 修为", task.userId(), task.expectedReward());
    }

    /**
     * 收获修炼(!收获):到期后收获全额收益(超时有溢出惩罚);
     * 也可提前结束——满 10 分钟后收益 = 预期 × 进度 × (0.5 + 0.5×进度),不足 10 分钟无任何收益。
     */
    public String harvest(Player player) {
        Optional<CultivationTask> opt = taskRepository.findActiveByUser(player.userId());
        if (opt.isEmpty()) return "道友当前没有可收获的修炼。";
        CultivationTask task = opt.get();
        long now = System.currentTimeMillis();
        if (now < task.endTime()) {
            long elapsedMinutes = (now - task.startTime()) / 60_000;
            if (elapsedMinutes < MIN_HARVEST_MINUTES) {
                return "修炼不足 " + MIN_HARVEST_MINUTES + " 分钟,提前收获不会获得任何收益,请继续修炼。";
            }
            double progress = (double) (now - task.startTime()) / (task.endTime() - task.startTime());
            long gain = Math.round(task.expectedReward() * progress * (0.5 + 0.5 * progress));
            String levelTip = playerService.gainExp(player, gain);
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
            return "道友提前结束修炼(进度 " + Math.round(progress * 100) + "%),获得 +" + gain
                    + " 修为(提前收获有衰减)\n当前" + progressOf(player) + levelTip;
        }
        if (isQuick(task.durationMinutes())) {
            settleQuick(task);
            return "道友修炼完成!收益已自动结算,当前"
                    + playerService.find(player.userId()).map(CultivationService::progressOf).orElse("");
        }

        long overtimeMinutes = (now - task.endTime()) / 60_000;
        long baseExp = task.expectedReward();
        StringBuilder msg = new StringBuilder();
        String levelTip;
        if (overtimeMinutes < 1) {
            levelTip = playerService.gainExp(player, baseExp);
            msg.append("道友修炼完成!获得 +").append(baseExp).append(" 修为");
        } else {
            long overflowExp = Math.min(baseExp, Math.round(overtimeMinutes / 60.0 * BASE_EXP_PER_HOUR));
            long gainExp = baseExp - overflowExp;
            levelTip = playerService.gainExp(player, gainExp);
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
        msg.append("\n当前").append(progressOf(player)).append(levelTip);
        return msg.toString();
    }

    /** 当前等级与修为进度描述。 */
    public static String progressOf(Player player) {
        if (player.level() >= LevelTable.MAX_LEVEL) {
            return "Lv." + player.level() + ",修为:" + player.exp() + "(已满级)";
        }
        String suffix = LevelTable.atBreakthrough(player.level()) ? "(需突破)" : "";
        return "Lv." + player.level() + ",修为:" + player.exp() + "/"
                + LevelTable.expToNextLevel(player.level()) + suffix;
    }

    public static String formatDuration(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) return (minutes / 60) + " 小时";
        if (minutes > 60) return (minutes / 60) + " 小时 " + (minutes % 60) + " 分钟";
        return minutes + " 分钟";
    }
}

package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 修炼系统(Phase 0 骨架):
 * - 快速修炼:默认 30 分钟,固定 +100 修为 +50 灵石
 * - 长时修炼:2-24 小时,时长 × 基础收益(溢出惩罚在 Phase 1 完善)
 */
public class CultivationService {
    private static final Logger log = LoggerFactory.getLogger(CultivationService.class);

    public static final int DEFAULT_MINUTES = 30;
    public static final long QUICK_EXP = 100;
    public static final long QUICK_STONES = 50;
    public static final long BASE_EXP_PER_HOUR = 60;

    private final CultivationTaskRepository taskRepository;
    private final PlayerService playerService;
    /** 修炼完成时的推送回调:(task, 结算文本) */
    private volatile BiConsumer<CultivationTask, String> notifier = (t, msg) -> log.info("[结算] {}: {}", t.userId(), msg);

    public CultivationService(CultivationTaskRepository taskRepository, PlayerService playerService) {
        this.taskRepository = taskRepository;
        this.playerService = playerService;
    }

    public void setNotifier(BiConsumer<CultivationTask, String> notifier) {
        this.notifier = notifier;
    }

    public Optional<CultivationTask> runningTask(String userId) {
        return taskRepository.findRunningByUser(userId);
    }

    /**
     * 开始修炼。
     *
     * @param minutes 修炼时长(分钟),默认 30
     */
    public String start(Player player, String method, int minutes) {
        if (taskRepository.findRunningByUser(player.userId()).isPresent()) {
            return "道友已在修炼中,请先等待本次修炼完成。";
        }
        if (minutes <= 0) minutes = DEFAULT_MINUTES;
        if (minutes > 24 * 60) return "单次修炼时长不可超过 24 小时。";
        long reward = expectedReward(minutes);
        taskRepository.insert(player.userId(), player.groupId(), method, System.currentTimeMillis(), minutes, reward);
        String methodDesc = method == null ? "" : "《" + method + "》";
        return "道友开始修炼" + methodDesc + ",预计 " + formatDuration(minutes) + " 后完成,将获得 +" + reward + " 修为";
    }

    public long expectedReward(int minutes) {
        if (minutes <= DEFAULT_MINUTES) return QUICK_EXP;
        return Math.round(minutes / 60.0 * BASE_EXP_PER_HOUR) + QUICK_EXP;
    }

    /**
     * 结算所有到期任务,由调度器周期调用。
     */
    public void settleFinishedTasks() {
        List<CultivationTask> finished = taskRepository.findFinishedRunning(System.currentTimeMillis());
        for (CultivationTask task : finished) {
            try {
                settle(task);
            } catch (Exception e) {
                log.error("结算修炼任务 {} 失败", task.taskId(), e);
            }
        }
    }

    private void settle(CultivationTask task) {
        Optional<Player> opt = playerService.find(task.userId());
        if (opt.isEmpty()) {
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_EXPIRED);
            return;
        }
        Player player = opt.get();
        long exp = task.expectedReward();
        long stones = task.durationMinutes() <= DEFAULT_MINUTES ? QUICK_STONES : 0;
        player.addExp(exp);
        player.addSpiritStones(stones);
        playerService.save(player);
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        String msg = "道友修炼完成!获得 +" + exp + " 修为"
                + (stones > 0 ? ",+" + stones + " 灵石" : "")
                + ",当前修为:" + player.exp() + "/" + player.realm().expToNext();
        notifier.accept(task, msg);
    }

    public static String formatDuration(int minutes) {
        if (minutes % 60 == 0) return (minutes / 60) + " 小时";
        if (minutes > 60) return (minutes / 60) + " 小时 " + (minutes % 60) + " 分钟";
        return minutes + " 分钟";
    }
}

package com.ocmptd.idlecultivator.scheduler;

import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.SectWarService;
import com.ocmptd.idlecultivator.game.social.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 游戏定时任务:
 * - 每 10 秒检查一次到期修炼任务并结算
 * - 每分钟检查灵气潮汐(20:00 触发)
 * - 每小时更新群活跃度等级、清理过期互助
 * - 每小时检查宗门战周结算(周一 0:00)
 */
public class GameScheduler implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GameScheduler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "game-scheduler");
        t.setDaemon(true);
        return t;
    });
    private final CultivationService cultivationService;
    private final GroupService groupService;
    private final SocialService socialService;
    private final SectWarService sectWarService;
    /** 记录上次结算的周键,用于检测跨周 */
    private volatile String lastSettledWeek;

    public GameScheduler(CultivationService cultivationService) {
        this(cultivationService, null, null, null);
    }

    public GameScheduler(CultivationService cultivationService, GroupService groupService,
                         SocialService socialService, SectWarService sectWarService) {
        this.cultivationService = cultivationService;
        this.groupService = groupService;
        this.socialService = socialService;
        this.sectWarService = sectWarService;
        this.lastSettledWeek = GroupService.currentWeekKey();
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::safeSettle, 10, 10, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(this::safeTideCheck, 60, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(this::safeHourlyTasks, 1, 1, TimeUnit.HOURS);
        log.info("定时任务已启动:修炼结算(10s) / 灵气潮汐(1min) / 小时任务(1h)");
    }

    private void safeSettle() {
        try {
            cultivationService.settleFinishedTasks();
        } catch (Exception e) {
            log.error("修炼结算轮询异常", e);
        }
    }

    private void safeTideCheck() {
        try {
            if (groupService != null) groupService.checkTide();
        } catch (Exception e) {
            log.error("灵气潮汐检查异常", e);
        }
    }

    private void safeHourlyTasks() {
        try {
            if (groupService != null) groupService.updateActivityLevels();
        } catch (Exception e) {
            log.error("群活跃度更新异常", e);
        }
        try {
            if (socialService != null) socialService.cleanExpiredHelps();
        } catch (Exception e) {
            log.error("清理过期互助异常", e);
        }
        try {
            checkWeeklySettlement();
        } catch (Exception e) {
            log.error("宗门战周结算检查异常", e);
        }
    }

    /** 检测跨周并执行宗门战结算。 */
    private void checkWeeklySettlement() {
        if (sectWarService == null) return;
        String currentWeek = GroupService.currentWeekKey();
        if (!currentWeek.equals(lastSettledWeek)) {
            log.info("检测到跨周({} → {}),执行宗门战结算", lastSettledWeek, currentWeek);
            String result = sectWarService.weeklySettlement();
            if (result != null) {
                log.info("宗门战结算结果:\n{}", result);
            }
            lastSettledWeek = currentWeek;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}

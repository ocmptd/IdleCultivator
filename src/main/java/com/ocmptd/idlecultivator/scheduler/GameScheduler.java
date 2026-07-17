package com.ocmptd.idlecultivator.scheduler;

import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 游戏定时任务:
 * - 每 10 秒检查一次到期修炼任务并结算
 * - 每小时更新一次群活跃度(Phase 3 实现)
 */
public class GameScheduler implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GameScheduler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "game-scheduler");
        t.setDaemon(true);
        return t;
    });
    private final CultivationService cultivationService;

    public GameScheduler(CultivationService cultivationService) {
        this.cultivationService = cultivationService;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::safeSettle, 10, 10, TimeUnit.SECONDS);
        log.info("定时任务已启动:修炼结算轮询(10s)");
    }

    private void safeSettle() {
        try {
            cultivationService.settleFinishedTasks();
        } catch (Exception e) {
            log.error("修炼结算轮询异常", e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}

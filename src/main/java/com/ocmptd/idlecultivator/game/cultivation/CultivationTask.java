package com.ocmptd.idlecultivator.game.cultivation;

/**
 * 修炼任务。status: 0 进行中 / 1 已完成 / 2 已过期。
 */
public record CultivationTask(
        long taskId,
        String userId,
        String groupId,
        String method,
        long startTime,
        int durationMinutes,
        int status,
        long expectedReward,
        double overflowPenalty) {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_DONE = 1;
    public static final int STATUS_EXPIRED = 2;

    public long endTime() {
        return startTime + durationMinutes * 60_000L;
    }
}

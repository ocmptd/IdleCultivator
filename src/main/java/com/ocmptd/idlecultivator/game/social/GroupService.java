package com.ocmptd.idlecultivator.game.social;

import com.ocmptd.idlecultivator.storage.GroupStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 群社交服务:群活跃度分级、灵气潮汐。
 * <p>
 * 活跃度分级(每小时根据消息数重新评估):
 * - 0 条消息 → 灵气稀薄(×0.8)
 * - 1~10 条 → 灵气平和(×1.0)
 * - 11~50 条 → 灵气充沛(×1.2)
 * - 50+ 条 → 灵气鼎盛(×1.5)
 * <p>
 * 灵气潮汐:每晚 20:00 触发,持续 1 小时,期间修炼效率翻倍(×2)。
 */
public class GroupService {
    private static final Logger log = LoggerFactory.getLogger(GroupService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 灵气潮汐触发时间(20:00) */
    private static final int TIDE_HOUR = 20;
    /** 潮汐持续时间(1 小时) */
    private static final long TIDE_DURATION_MS = 60 * 60_000L;

    private final GroupStatusRepository repository;

    public GroupService(GroupStatusRepository repository) {
        this.repository = repository;
    }

    /** 记录群消息(由 BotService 在每条群消息时调用)。 */
    public void recordMessage(String groupId) {
        if (groupId == null) return;
        repository.incrementMessageCount(groupId);
    }

    /** 获取群状态(不存在则创建)。 */
    public GroupStatus getGroupStatus(String groupId) {
        if (groupId == null) return new GroupStatus(null);
        return repository.getOrCreate(groupId);
    }

    /**
     * 计算群相关的修炼效率倍率 = 群活跃度倍率 × 潮汐倍率。
     * groupId 为 null 时返回 1.0。
     */
    public double groupMultiplier(String groupId) {
        if (groupId == null) return 1.0;
        GroupStatus gs = getGroupStatus(groupId);
        double multiplier = gs.activityMultiplier();
        if (gs.isTideActive(System.currentTimeMillis())) {
            multiplier *= 2.0;
        }
        return multiplier;
    }

    /**
     * 每小时更新所有群的活跃度等级(由调度器调用)。
     * 根据上一小时的 hourly_msg_count 重新分级,然后重置计数器。
     */
    public void updateActivityLevels() {
        // 活跃度分级逻辑在 save 时处理,这里由 repository 调用
        // 实际逻辑:遍历所有群,根据 hourly_msg_count 重新分级
        for (GroupStatus gs : repository.findAll()) {
            int oldLevel = gs.activityLevel();
            int newLevel = computeActivityLevel(gs.hourlyMsgCount());
            if (newLevel != oldLevel) {
                gs.setActivityLevel(newLevel);
                log.info("群 {} 活跃度变化:等级{} → 等级{}", gs.groupId(), oldLevel, newLevel);
            }
            gs.setHourlyMsgCount(0);
            repository.save(gs);
        }
    }

    /** 根据小时消息数计算活跃度等级。 */
    public static int computeActivityLevel(int hourlyMsgCount) {
        if (hourlyMsgCount == 0) return 0;       // 灵气稀薄
        if (hourlyMsgCount <= 10) return 1;      // 灵气平和
        if (hourlyMsgCount <= 50) return 2;      // 灵气充沛
        return 3;                                 // 灵气鼎盛
    }

    /**
     * 检查并触发灵气潮汐(由调度器每分钟调用)。
     * 每天 20:00 触发一次,持续 1 小时。
     */
    public void checkTide() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (now.getHour() != TIDE_HOUR) return;

        String today = now.format(DATE_FMT);
        // 遍历所有群,如果今天还没触发潮汐则触发
        for (GroupStatus gs : repository.findAll()) {
            if (!today.equals(gs.lastTideDate())) {
                long nowMs = System.currentTimeMillis();
                gs.setTideUntil(nowMs + TIDE_DURATION_MS);
                gs.setLastTideDate(today);
                repository.save(gs);
                log.info("群 {} 灵气潮汐已触发,持续至 {}", gs.groupId(),
                        LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(gs.tideUntil()), ZONE));
            }
        }
    }

    /** 群状态描述文案。 */
    public String describe(GroupStatus gs) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 群状态 ===\n");
        sb.append("活跃度:").append(gs.activityLabel());
        sb.append("(修炼效率×").append(gs.activityMultiplier()).append(")\n");
        sb.append("总消息数:").append(gs.messageCount());
        sb.append(",本小时消息:").append(gs.hourlyMsgCount());
        if (gs.isTideActive(System.currentTimeMillis())) {
            long remain = (gs.tideUntil() - System.currentTimeMillis()) / 60_000;
            sb.append("\n🌊 灵气潮汐进行中!修炼效率翻倍,剩余约 ").append(remain).append(" 分钟");
        } else {
            sb.append("\n灵气潮汐:每晚 20:00 开启");
        }
        return sb.toString();
    }

    /** 获取当前 ISO 周键(如 "2026-W29"),用于宗门战按周统计。 */
    public static String currentWeekKey() {
        LocalDate now = LocalDate.now(ZONE);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.get(weekFields.weekBasedYear());
        return String.format("%d-W%02d", year, weekNumber);
    }

    /** 获取上周 ISO 周键。 */
    public static String lastWeekKey() {
        LocalDate now = LocalDate.now(ZONE).minusWeeks(1);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.get(weekFields.weekBasedYear());
        return String.format("%d-W%02d", year, weekNumber);
    }

    /** 下一波潮汐的开始时间(用于提示文案)。 */
    public static String nextTideTime() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime next = now.withHour(TIDE_HOUR).withMinute(0).withSecond(0).withNano(0);
        if (now.isAfter(next)) next = next.plusDays(1);
        return next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
}

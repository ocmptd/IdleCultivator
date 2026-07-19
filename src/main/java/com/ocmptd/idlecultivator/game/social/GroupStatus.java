package com.ocmptd.idlecultivator.game.social;

/**
 * 群状态:活跃度分级与灵气潮汐。
 * activityLevel: 0=灵气稀薄(×0.8) / 1=灵气平和(×1.0) / 2=灵气充沛(×1.2) / 3=灵气鼎盛(×1.5)
 */
public class GroupStatus {
    private final String groupId;
    private long lastActiveTime;
    private int messageCount;
    private int activityLevel = 1;
    private int hourlyMsgCount;
    private long tideUntil;
    private String lastTideDate;

    public GroupStatus(String groupId) {
        this.groupId = groupId;
    }

    public String groupId() {
        return groupId;
    }

    public long lastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public int messageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int activityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(int activityLevel) {
        this.activityLevel = activityLevel;
    }

    public int hourlyMsgCount() {
        return hourlyMsgCount;
    }

    public void setHourlyMsgCount(int hourlyMsgCount) {
        this.hourlyMsgCount = hourlyMsgCount;
    }

    public long tideUntil() {
        return tideUntil;
    }

    public void setTideUntil(long tideUntil) {
        this.tideUntil = tideUntil;
    }

    public String lastTideDate() {
        return lastTideDate;
    }

    public void setLastTideDate(String lastTideDate) {
        this.lastTideDate = lastTideDate;
    }

    /** 灵气潮汐是否处于激活状态。 */
    public boolean isTideActive(long now) {
        return tideUntil > now;
    }

    /** 活跃度等级对应的修炼效率倍率。 */
    public double activityMultiplier() {
        return switch (activityLevel) {
            case 0 -> 0.8;
            case 2 -> 1.2;
            case 3 -> 1.5;
            default -> 1.0;
        };
    }

    /** 活跃度等级文字描述。 */
    public String activityLabel() {
        return switch (activityLevel) {
            case 0 -> "灵气稀薄";
            case 2 -> "灵气充沛";
            case 3 -> "灵气鼎盛";
            default -> "灵气平和";
        };
    }
}

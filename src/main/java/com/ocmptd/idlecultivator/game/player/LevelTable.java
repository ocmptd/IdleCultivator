package com.ocmptd.idlecultivator.game.player;

/**
 * 等级体系:80 级,4 个阶段(每 20 级一个阶段,与境界合并)。
 * 节奏(按日均约 2000 修为):练气 ~2 天,筑基 ~20 天,金丹+元婴 ~40 天。
 * 阶段末(20/40/60 级)需突破才能进入下一阶段;80 级满级。
 */
public final class LevelTable {
    public static final int MAX_LEVEL = 80;
    public static final int STAGE_SIZE = 20;

    private LevelTable() {
    }

    /** 等级所在阶段的境界(1~20 练气 / 21~40 筑基 / 41~60 金丹 / 61~80 元婴)。 */
    public static Realm realmOf(int level) {
        int stage = (clamp(level) - 1) / STAGE_SIZE;
        return switch (stage) {
            case 0 -> Realm.LIAN_QI;
            case 1 -> Realm.ZHU_JI;
            case 2 -> Realm.JIN_DAN;
            default -> Realm.YUAN_YING;
        };
    }

    /** 是否处于阶段末卡点(20/40/60 级),需突破才能继续升级。 */
    public static boolean atBreakthrough(int level) {
        return level < MAX_LEVEL && level % STAGE_SIZE == 0;
    }

    /**
     * 从 level 升到 level+1 所需修为(分段线性,常量集中便于调参)。
     * 练气 1~19:100+10×级;筑基 21~39:1200+80×阶段内级;
     * 金丹 41~59:1600+60×阶段内级;元婴 61~79:2800+100×阶段内级;
     * 阶段末(20/40/60)为突破消耗。满级返回 Long.MAX_VALUE。
     */
    public static long expToNextLevel(int level) {
        level = clamp(level);
        if (level >= MAX_LEVEL) return Long.MAX_VALUE;
        int inStage = ((level - 1) % STAGE_SIZE) + 1;
        return switch ((level - 1) / STAGE_SIZE) {
            case 0 -> 100L + 10L * inStage;
            case 1 -> 1200L + 80L * inStage;
            case 2 -> 1600L + 60L * inStage;
            default -> 2800L + 100L * inStage;
        };
    }

    /** 元婴期境界压制:修炼收益 ×0.8。 */
    public static double cultivationMultiplier(int level) {
        return realmOf(level) == Realm.YUAN_YING ? 0.8 : 1.0;
    }

    /**
     * 消耗修为自动升级(可连升),在阶段末卡点或满级时停止。
     * @return 升级次数(0 表示未升级)
     */
    public static int levelUp(Player p) {
        int gained = 0;
        while (p.level() < MAX_LEVEL && !atBreakthrough(p.level())) {
            long cost = expToNextLevel(p.level());
            if (p.exp() < cost) break;
            p.setExp(p.exp() - cost);
            p.setLevel(p.level() + 1);
            p.setRealm(realmOf(p.level()));
            gained++;
        }
        return gained;
    }

    /** 老数据折算:按已有境界给初始等级。 */
    public static int initialLevelOf(Realm realm) {
        return switch (realm) {
            case LIAN_QI -> 1;
            case ZHU_JI -> 21;
            case JIN_DAN -> 41;
            default -> 61;
        };
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(level, MAX_LEVEL));
    }
}

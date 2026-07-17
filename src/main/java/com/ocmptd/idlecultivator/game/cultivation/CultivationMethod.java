package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.player.Realm;

/**
 * 内置功法:各有收益倍率与境界要求,修炼时可指定(默认吐纳诀)。
 */
public enum CultivationMethod {
    TU_NA("吐纳诀", 1.0, Realm.LIAN_QI),
    ZI_FU("紫府诀", 1.15, Realm.ZHU_JI),
    XUAN_TIAN("玄天功", 1.3, Realm.JIN_DAN),
    TAI_XU("太虚剑意", 1.5, Realm.YUAN_YING);

    private final String label;
    private final double multiplier;
    private final Realm requiredRealm;

    CultivationMethod(String label, double multiplier, Realm requiredRealm) {
        this.label = label;
        this.multiplier = multiplier;
        this.requiredRealm = requiredRealm;
    }

    public String label() {
        return label;
    }

    public double multiplier() {
        return multiplier;
    }

    public Realm requiredRealm() {
        return requiredRealm;
    }

    public static CultivationMethod fromLabel(String label) {
        for (CultivationMethod m : values()) {
            if (m.label.equals(label)) return m;
        }
        return null;
    }

    public static String describeAll() {
        StringBuilder sb = new StringBuilder("=== 功法一览 ===");
        for (CultivationMethod m : values()) {
            sb.append("\n《").append(m.label).append("》收益×").append(m.multiplier)
                    .append(",需").append(m.requiredRealm.label()).append("及以上");
        }
        return sb.toString();
    }
}

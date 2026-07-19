package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.player.CultivationDirection;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Realm;

/**
 * 内置功法:各有收益倍率与境界要求,修炼时可指定(默认吐纳诀)。
 * Phase 2 新增性别×方向联动功法:焚天诀/九霄云衣诀/丹心诀/冰魄剑诀。
 */
public enum CultivationMethod {
    TU_NA("吐纳诀", 1.0, Realm.LIAN_QI, null, null),
    ZI_FU("紫府诀", 1.15, Realm.ZHU_JI, null, null),
    XUAN_TIAN("玄天功", 1.3, Realm.JIN_DAN, null, null),
    TAI_XU("太虚剑意", 1.5, Realm.YUAN_YING, null, null),
    // 性别×方向联动功法
    FEN_TIAN("焚天诀", 1.4, Realm.ZHU_JI, Gender.MALE, CultivationDirection.SWORD),
    JIU_XIAO("九霄云衣诀", 1.4, Realm.ZHU_JI, Gender.FEMALE, CultivationDirection.MAGIC),
    DAN_XIN("丹心诀", 1.35, Realm.ZHU_JI, null, CultivationDirection.MEDICINE),
    BING_PO("冰魄剑诀", 1.5, Realm.JIN_DAN, Gender.FEMALE, CultivationDirection.SWORD);

    private final String label;
    private final double multiplier;
    private final Realm requiredRealm;
    private final Gender genderReq;
    private final CultivationDirection directionReq;

    CultivationMethod(String label, double multiplier, Realm requiredRealm,
                      Gender genderReq, CultivationDirection directionReq) {
        this.label = label;
        this.multiplier = multiplier;
        this.requiredRealm = requiredRealm;
        this.genderReq = genderReq;
        this.directionReq = directionReq;
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

    public Gender genderReq() {
        return genderReq;
    }

    public CultivationDirection directionReq() {
        return directionReq;
    }

    /** 是否为性别×方向联动功法。 */
    public boolean isLinked() {
        return genderReq != null || directionReq != null;
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
            if (m.genderReq != null) sb.append(",限").append(m.genderReq.label());
            if (m.directionReq != null) sb.append(",").append(m.directionReq.label()).append("专属");
        }
        return sb.toString();
    }
}

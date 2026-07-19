package com.ocmptd.idlecultivator.game.player;

/**
 * 修炼方向,筑基期后可选。
 */
public enum CultivationDirection {
    SWORD("剑修", "攻击+15%，可习剑道功法"),
    MAGIC("法修", "神识+15%，修炼效率+5%"),
    MEDICINE("医修", "气血+10%，修炼效率+5%");

    private final String label;
    private final String bonusDesc;

    CultivationDirection(String label, String bonusDesc) {
        this.label = label;
        this.bonusDesc = bonusDesc;
    }

    public String label() {
        return label;
    }

    public String bonusDesc() {
        return bonusDesc;
    }

    /** 修炼效率加成倍率。 */
    public double cultivationBonus() {
        return switch (this) {
            case SWORD -> 1.0;
            case MAGIC -> 1.05;
            case MEDICINE -> 1.05;
        };
    }

    public static CultivationDirection fromLabel(String label) {
        for (CultivationDirection d : values()) {
            if (d.label.equals(label)) return d;
        }
        return null;
    }

    public static String describeAll() {
        StringBuilder sb = new StringBuilder("=== 修炼方向 ===");
        for (CultivationDirection d : values()) {
            sb.append("\n").append(d.label).append("—— ").append(d.bonusDesc);
        }
        return sb.toString();
    }
}

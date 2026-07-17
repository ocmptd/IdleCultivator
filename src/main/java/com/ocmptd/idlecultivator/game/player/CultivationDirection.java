package com.ocmptd.idlecultivator.game.player;

/**
 * 修炼方向,筑基期后可选。
 */
public enum CultivationDirection {
    SWORD("剑修"),
    MAGIC("法修"),
    MEDICINE("医修");

    private final String label;

    CultivationDirection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CultivationDirection fromLabel(String label) {
        for (CultivationDirection d : values()) {
            if (d.label.equals(label)) return d;
        }
        return null;
    }
}

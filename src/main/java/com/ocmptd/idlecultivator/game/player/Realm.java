package com.ocmptd.idlecultivator.game.player;

/**
 * 修仙境界,按顺序递进,expToNext 为突破至下一境界所需修为。
 */
public enum Realm {
    LIAN_QI("练气期", 200),
    ZHU_JI("筑基期", 500),
    JIN_DAN("金丹期", 1000),
    YUAN_YING("元婴期", 3000),
    HUA_SHEN("化神期", 8000),
    DA_CHENG("大乘期", 20000);

    private final String label;
    private final int expToNext;

    Realm(String label, int expToNext) {
        this.label = label;
        this.expToNext = expToNext;
    }

    public String label() {
        return label;
    }

    public int expToNext() {
        return expToNext;
    }

    public Realm next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : this;
    }

    public static Realm fromLabel(String label) {
        for (Realm r : values()) {
            if (r.label.equals(label)) return r;
        }
        return LIAN_QI;
    }
}

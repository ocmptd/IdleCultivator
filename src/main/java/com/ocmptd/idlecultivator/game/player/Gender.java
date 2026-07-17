package com.ocmptd.idlecultivator.game.player;

public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Gender fromLabel(String label) {
        for (Gender g : values()) {
            if (g.label.equals(label)) return g;
        }
        return null;
    }
}

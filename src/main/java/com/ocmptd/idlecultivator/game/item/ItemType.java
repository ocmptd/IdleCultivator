package com.ocmptd.idlecultivator.game.item;

/**
 * 道具类型。
 */
public enum ItemType {
    PILL("丹药"),
    CONSUMABLE("消耗品"),
    MATERIAL("材料"),
    EQUIPMENT("装备");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

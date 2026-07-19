package com.ocmptd.idlecultivator.game.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 道具注册表：定义所有道具的名称、类型、描述、价格与是否可购买。
 */
public final class ItemRegistry {
    private static final Map<String, ItemDef> ITEMS = new LinkedHashMap<>();

    public record ItemDef(String name, ItemType type, String description, int price, boolean shopAvailable) {}

    static {
        register("加速丹", ItemType.CONSUMABLE, "立即完成当前修炼，获得全额收益", 200, true);
        register("聚灵丹", ItemType.PILL, "立即获得50修为", 80, true);
        register("筑基丹", ItemType.PILL, "突破时消耗，成功率提升至95%", 300, true);
        register("回气丹", ItemType.PILL, "立即获得100修为", 120, true);
        register("灵尘", ItemType.MATERIAL, "修炼溢出转化的灵气尘埃", 10, false);
        register("残破法宝", ItemType.MATERIAL, "损坏的法宝碎片，蕴含残余灵力", 30, false);
        register("木剑", ItemType.EQUIPMENT, "新手木剑，朴实无华", 0, false);
    }

    private ItemRegistry() {}

    private static void register(String name, ItemType type, String description, int price, boolean shopAvailable) {
        ITEMS.put(name, new ItemDef(name, type, description, price, shopAvailable));
    }

    public static ItemDef get(String name) {
        return ITEMS.get(name);
    }

    public static boolean exists(String name) {
        return ITEMS.containsKey(name);
    }

    public static List<ItemDef> shopItems() {
        return ITEMS.values().stream().filter(ItemDef::shopAvailable).toList();
    }

    public static String describeShop() {
        StringBuilder sb = new StringBuilder("=== 灵宝阁 ===");
        for (ItemDef item : shopItems()) {
            sb.append("\n【").append(item.name()).append("】").append(item.type().label())
                    .append(" —— ").append(item.description())
                    .append(" | 价格:").append(item.price()).append("灵石");
        }
        return sb.toString();
    }
}

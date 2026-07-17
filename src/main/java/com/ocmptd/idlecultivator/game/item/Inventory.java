package com.ocmptd.idlecultivator.game.item;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 背包工具:以 "名称×数量;名称×数量" 文本与 Map 互转,存于 users.inventory。
 */
public final class Inventory {
    private Inventory() {
    }

    public static Map<String, Integer> parse(String text) {
        Map<String, Integer> items = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return items;
        for (String part : text.split(";")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int idx = p.lastIndexOf('×');
            if (idx < 0) {
                items.merge(p, 1, Integer::sum);
            } else {
                String name = p.substring(0, idx).trim();
                int count;
                try {
                    count = Integer.parseInt(p.substring(idx + 1).trim());
                } catch (NumberFormatException e) {
                    count = 1;
                }
                if (!name.isEmpty() && count > 0) items.merge(name, count, Integer::sum);
            }
        }
        return items;
    }

    public static String serialize(Map<String, Integer> items) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(e.getKey()).append("×").append(e.getValue());
        }
        return sb.toString();
    }

    public static String add(String text, String name, int count) {
        Map<String, Integer> items = parse(text);
        items.merge(name, count, Integer::sum);
        return serialize(items);
    }

    /** 消耗一个物品,成功返回新背包文本,数量不足返回 null。 */
    public static String consume(String text, String name, int count) {
        Map<String, Integer> items = parse(text);
        Integer have = items.get(name);
        if (have == null || have < count) return null;
        if (have == count) items.remove(name);
        else items.put(name, have - count);
        return serialize(items);
    }

    public static String display(String text) {
        Map<String, Integer> items = parse(text);
        if (items.isEmpty()) return "空空如也";
        return String.join(",", items.entrySet().stream()
                .map(e -> e.getKey() + "×" + e.getValue()).toList());
    }
}

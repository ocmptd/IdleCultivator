package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.player.Realm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 形象定制：!换发型 / !换服饰 / !换配饰
 * 境界解锁进阶外观，首次免费，更换需花费灵石。
 */
public class AppearanceCommand implements Command {
    /** 还原默认外观的关键词(免费清空当前槽位)。 */
    private static final Set<String> RESET_KEYWORDS = Set.of("默认", "无", "卸下", "还原");
    private static final Map<String, Option> HAIRSTYLES = new LinkedHashMap<>();
    private static final Map<String, Option> OUTFITS = new LinkedHashMap<>();
    private static final Map<String, Option> ACCESSORIES = new LinkedHashMap<>();

    static {
        HAIRSTYLES.put("素髻", new Option(Realm.LIAN_QI, 0));
        HAIRSTYLES.put("道髻", new Option(Realm.ZHU_JI, 50));
        HAIRSTYLES.put("飞仙髻", new Option(Realm.JIN_DAN, 100));
        HAIRSTYLES.put("凌云髻", new Option(Realm.YUAN_YING, 200));

        OUTFITS.put("麻衣", new Option(Realm.LIAN_QI, 0));
        OUTFITS.put("道袍", new Option(Realm.ZHU_JI, 100));
        OUTFITS.put("法袍", new Option(Realm.JIN_DAN, 200));
        OUTFITS.put("仙衣", new Option(Realm.YUAN_YING, 400));

        ACCESSORIES.put("玉佩", new Option(Realm.ZHU_JI, 80));
        ACCESSORIES.put("灵环", new Option(Realm.JIN_DAN, 150));
        ACCESSORIES.put("宝冠", new Option(Realm.YUAN_YING, 300));
    }

    private record Option(Realm requiredRealm, int cost) {}

    private final String commandName;
    private final String category;
    private final PlayerService playerService;

    public AppearanceCommand(String commandName, String category, PlayerService playerService) {
        this.commandName = commandName;
        this.category = category;
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return commandName;
    }

    @Override
    public String usage() {
        return commandName + " [名称] —— 更换" + category + "(境界解锁,更换需灵石;输入\"默认\"可免费还原)";
    }

    @Override
    public boolean inlineArgs() {
        return true;
    }

    private Map<String, Option> options() {
        return switch (category) {
            case "发型" -> HAIRSTYLES;
            case "服饰" -> OUTFITS;
            case "配饰" -> ACCESSORIES;
            default -> Map.of();
        };
    }

    private String current(Player p) {
        return switch (category) {
            case "发型" -> p.hairstyle();
            case "服饰" -> p.outfit();
            case "配饰" -> p.accessory();
            default -> null;
        };
    }

    private void apply(Player p, String value) {
        switch (category) {
            case "发型" -> p.setHairstyle(value);
            case "服饰" -> p.setOutfit(value);
            case "配饰" -> p.setAccessory(value);
        }
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();

        String arg = ctx.arg(0);
        Map<String, Option> opts = options();

        if (arg == null || arg.isBlank()) {
            StringBuilder sb = new StringBuilder("=== " + category + "定制 ===");
            String cur = current(p);
            for (var entry : opts.entrySet()) {
                Option o = entry.getValue();
                String status = entry.getKey().equals(cur) ? " (当前)" : "";
                String lock = p.realm().ordinal() < o.requiredRealm.ordinal()
                        ? " [需" + o.requiredRealm.label() + "]"
                        : (o.cost > 0 ? " [" + o.cost + "灵石]" : " [免费]");
                sb.append("\n").append(entry.getKey()).append(lock).append(status);
            }
            sb.append("\n\n输入 ").append(ctx.prefix()).append(commandName).append(" [名称] 更换")
                    .append(",或 ").append(ctx.prefix()).append(commandName).append(" 默认 免费还原默认外观");
            return sb.toString();
        }

        // 还原默认外观:清空当前槽位,免费
        if (RESET_KEYWORDS.contains(arg)) {
            String cur = current(p);
            if (cur == null) {
                return "道友当前" + category + "已是默认外观,无需还原。";
            }
            apply(p, null);
            playerService.save(p);
            return category + "已还原为默认外观!\n输入 " + ctx.prefix() + "状态 查看角色形象。";
        }

        Option option = opts.get(arg);
        if (option == null) {
            return "未知" + category + ":" + arg + "\n可选:" + String.join("/", opts.keySet());
        }

        if (p.realm().ordinal() < option.requiredRealm.ordinal()) {
            return arg + "需" + option.requiredRealm.label() + "及以上方可解锁,道友当前为" + p.realm().label() + "。";
        }

        String cur = current(p);
        if (arg.equals(cur)) {
            return "道友当前已是" + arg + ",无需更换。";
        }

        // 首次免费，后续更换需花费
        int cost = cur == null ? 0 : option.cost;
        if (cost > 0 && p.spiritStones() < cost) {
            return "更换" + category + "需" + cost + "灵石,道友当前仅有" + p.spiritStones() + "灵石。";
        }
        if (cost > 0) {
            p.setSpiritStones(p.spiritStones() - cost);
        }

        apply(p, arg);
        playerService.save(p);

        String msg = category + "已更换为「" + arg + "」!";
        if (cost > 0) {
            msg += "(消耗" + cost + "灵石,剩余" + p.spiritStones() + "灵石)";
        }
        msg += "\n输入 " + ctx.prefix() + "状态 查看角色形象。";
        return msg;
    }
}

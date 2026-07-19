package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.item.ItemRegistry;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

/**
 * 使用道具：!使用 [道具名]
 * 加速丹 → 立即完成修炼
 * 聚灵丹 → 获得50修为
 * 回气丹 → 获得100修为
 */
public class UseItemCommand implements Command {
    private final PlayerService playerService;
    private final CultivationService cultivationService;

    public UseItemCommand(PlayerService playerService, CultivationService cultivationService) {
        this.playerService = playerService;
        this.cultivationService = cultivationService;
    }

    @Override
    public String name() {
        return "使用";
    }

    @Override
    public String usage() {
        return "使用 [道具名] —— 使用背包中的道具";
    }

    @Override
    public boolean inlineArgs() {
        return true;
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();

        String itemName = ctx.arg(0);
        if (itemName == null || itemName.isBlank()) {
            return "请指定要使用的道具:" + ctx.prefix() + "使用 [道具名]";
        }

        // 检查背包中是否有该道具
        String newInventory = Inventory.consume(p.inventory(), itemName, 1);
        if (newInventory == null) {
            return "背包中没有【" + itemName + "】。";
        }

        // 根据道具类型执行效果
        return switch (itemName) {
            case "加速丹" -> {
                p.setInventory(newInventory);
                playerService.save(p);
                yield cultivationService.speedFinish(playerService.find(ctx.userId()).orElse(p));
            }
            case "聚灵丹" -> {
                p.setInventory(newInventory);
                String tip = playerService.gainExp(p, 50);
                yield "服下聚灵丹,获得50修为!" + tip;
            }
            case "回气丹" -> {
                p.setInventory(newInventory);
                String tip = playerService.gainExp(p, 100);
                yield "服下回气丹,获得100修为!" + tip;
            }
            case "筑基丹" ->
                    "筑基丹需在突破时使用,请输入 " + ctx.prefix() + "突破 用丹";
            default -> {
                ItemRegistry.ItemDef def = ItemRegistry.get(itemName);
                if (def != null && def.type() == com.ocmptd.idlecultivator.game.item.ItemType.MATERIAL) {
                    yield "【" + itemName + "】为材料,无法直接使用。";
                }
                yield "【" + itemName + "】暂无法直接使用。";
            }
        };
    }
}

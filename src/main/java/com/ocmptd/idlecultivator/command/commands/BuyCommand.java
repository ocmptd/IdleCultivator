package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.item.ItemRegistry;
import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

/**
 * 购买道具：!购买 [道具名] [数量]
 */
public class BuyCommand implements Command {
    private final PlayerService playerService;

    public BuyCommand(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return "购买";
    }

    @Override
    public String usage() {
        return "购买 [道具名] [数量] —— 用灵石购买道具";
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
            return "请指定要购买的道具:" + ctx.prefix() + "购买 [道具名] [数量]\n" + ItemRegistry.describeShop();
        }

        ItemRegistry.ItemDef item = ItemRegistry.get(itemName);
        if (item == null || !item.shopAvailable()) {
            return "该道具无法购买:" + itemName + "\n" + ItemRegistry.describeShop();
        }

        int count = 1;
        String countArg = ctx.arg(1);
        if (countArg != null) {
            try {
                count = Integer.parseInt(countArg);
            } catch (NumberFormatException e) {
                return "数量必须为数字。";
            }
        }
        if (count <= 0) return "购买数量必须大于0。";

        long totalCost = (long) item.price() * count;
        if (p.spiritStones() < totalCost) {
            return "灵石不足!需要" + totalCost + "灵石,当前仅有" + p.spiritStones() + "灵石。";
        }

        p.setSpiritStones(p.spiritStones() - totalCost);
        p.setInventory(Inventory.add(p.inventory(), itemName, count));
        playerService.save(p);

        return "购买成功!【" + itemName + "】×" + count + "(消耗" + totalCost + "灵石,剩余" + p.spiritStones() + "灵石)";
    }
}

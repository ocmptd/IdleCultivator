package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.item.ItemRegistry;

/**
 * 查看灵宝阁（道具商店）。
 */
public class ShopCommand implements Command {
    @Override
    public String name() {
        return "商店";
    }

    @Override
    public String usage() {
        return "商店 —— 查看可购买的道具";
    }

    @Override
    public String execute(CommandContext ctx) {
        return ItemRegistry.describeShop()
                + "\n\n输入 " + ctx.prefix() + "购买 [道具名] [数量] 购买道具";
    }
}

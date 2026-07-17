package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

public class BagCommand implements Command {
    private final PlayerService playerService;

    public BagCommand(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return "背包";
    }

    @Override
    public String usage() {
        return "背包 —— 查看背包物品与灵石";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();
        return "=== 道友的背包 ===\n物品:" + Inventory.display(p.inventory())
                + "\n灵石:" + p.spiritStones()
                + "\n装备:" + (p.equipment().isEmpty() ? "无" : p.equipment());
    }
}

package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

public class HarvestCommand implements Command {
    private final PlayerService playerService;
    private final CultivationService cultivationService;

    public HarvestCommand(PlayerService playerService, CultivationService cultivationService) {
        this.playerService = playerService;
        this.cultivationService = cultivationService;
    }

    @Override
    public String name() {
        return "收获";
    }

    @Override
    public String usage() {
        return "收获 —— 收获已完成的长时修炼(快速修炼自动收获)";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        return cultivationService.harvest(opt.get());
    }
}

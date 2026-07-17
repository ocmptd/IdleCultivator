package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.breakthrough.BreakthroughService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

public class BreakthroughCommand implements Command {
    private final PlayerService playerService;
    private final BreakthroughService breakthroughService;

    public BreakthroughCommand(PlayerService playerService, BreakthroughService breakthroughService) {
        this.playerService = playerService;
        this.breakthroughService = breakthroughService;
    }

    @Override
    public String name() {
        return "突破";
    }

    @Override
    public String usage() {
        return "突破 —— 修为足够时突破至下一境界";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        return breakthroughService.attempt(opt.get());
    }
}

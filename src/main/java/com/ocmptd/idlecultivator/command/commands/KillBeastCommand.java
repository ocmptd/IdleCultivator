package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.social.BeastService;

/**
 * !击杀 —— 攻击群内出现的异兽。
 */
public class KillBeastCommand implements Command {
    private final BeastService beastService;

    public KillBeastCommand(BeastService beastService) {
        this.beastService = beastService;
    }

    @Override
    public String name() {
        return "击杀";
    }

    @Override
    public String usage() {
        return "击杀 —— 攻击群内出现的异兽,击杀者获得修为与灵石";
    }

    @Override
    public String execute(CommandContext ctx) {
        return beastService.attack(ctx.userId(), ctx.groupId());
    }
}

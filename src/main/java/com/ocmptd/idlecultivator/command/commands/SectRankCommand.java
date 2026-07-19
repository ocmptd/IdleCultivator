package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.social.SectWarService;

/**
 * !宗门排行 —— 查看本周宗门战排行(以 QQ 群为单位比拼总修为)。
 */
public class SectRankCommand implements Command {
    private final SectWarService sectWarService;

    public SectRankCommand(SectWarService sectWarService) {
        this.sectWarService = sectWarService;
    }

    @Override
    public String name() {
        return "宗门排行";
    }

    @Override
    public String usage() {
        return "宗门排行 —— 查看本周宗门战排行(各群修炼总修为比拼)";
    }

    @Override
    public String execute(CommandContext ctx) {
        return sectWarService.describeRanking(ctx.groupId());
    }
}

package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.social.SocialService;

import java.util.Optional;

/**
 * !互助 @某人 —— 与对方结成互助,双方修炼效率+20%/1h,最多叠3次。
 */
public class MutualHelpCommand implements Command {
    private final PlayerService playerService;
    private final SocialService socialService;

    public MutualHelpCommand(PlayerService playerService, SocialService socialService) {
        this.playerService = playerService;
        this.socialService = socialService;
    }

    @Override
    public String name() {
        return "互助";
    }

    @Override
    public String usage() {
        return "互助 @某人 —— 与对方结成互助,双方修炼效率+20%/1h(最多叠3次)";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        String targetId = ctx.firstMentionedUserId();
        return socialService.requestHelp(ctx.userId(), targetId, ctx.groupId());
    }
}

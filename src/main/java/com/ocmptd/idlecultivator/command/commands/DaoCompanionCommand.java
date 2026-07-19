package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.social.SocialService;

import java.util.Optional;

/**
 * !道侣 @某人 —— 结为道侣,双方修炼效率永久+10%。
 * !道侣 解除 —— 解除道侣关系。
 * !道侣 —— 查看当前道侣。
 */
public class DaoCompanionCommand implements Command {
    private final PlayerService playerService;
    private final SocialService socialService;

    public DaoCompanionCommand(PlayerService playerService, SocialService socialService) {
        this.playerService = playerService;
        this.socialService = socialService;
    }

    @Override
    public String name() {
        return "道侣";
    }

    @Override
    public String usage() {
        return "道侣 @某人 —— 结为道侣(效率+10%);道侣 解除 —— 解除关系;道侣 —— 查看道侣";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";

        String arg = ctx.arg(0);
        if ("解除".equals(arg)) {
            return socialService.breakDaoCompanion(ctx.userId());
        }

        // 查看当前道侣
        Optional<String> partner = socialService.findPartnerId(ctx.userId());
        if (ctx.firstMentionedUserId() == null) {
            if (partner.isPresent()) {
                Player partnerPlayer = playerService.find(partner.get()).orElse(null);
                String partnerName = partnerPlayer == null ? partner.get()
                        : (partnerPlayer.name() == null ? partner.get() : partnerPlayer.name());
                return "道友的道侣是[" + partnerName + "],修炼效率永久+10%";
            }
            return "道友当前没有道侣。使用 " + ctx.prefix() + "道侣 @某人 结为道侣。";
        }

        // 结为道侣
        String targetId = ctx.firstMentionedUserId();
        return socialService.establishDaoCompanion(ctx.userId(), targetId, ctx.groupId());
    }
}

package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.portrait.PortraitService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

/**
 * 展示角色/道具的图片描述词(AI 绘图 prompt),暂不实际生成图片。
 */
public class PortraitCommand implements Command {
    private final PlayerService playerService;
    private final PortraitService portraitService;

    public PortraitCommand(PlayerService playerService, PortraitService portraitService) {
        this.playerService = playerService;
        this.portraitService = portraitService;
    }

    @Override
    public String name() {
        return "形象描述";
    }

    @Override
    public String usage() {
        return "形象描述 [道具] —— 查看角色(或背包道具)的图片描述词";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();
        if ("道具".equals(ctx.arg(0))) {
            return portraitService.allItemPrompts(p);
        }
        return portraitService.describeAppearance(p)
                + "\n\n=== 图片描述词(暂不生成图片) ===\n" + portraitService.imagePrompt(p);
    }
}

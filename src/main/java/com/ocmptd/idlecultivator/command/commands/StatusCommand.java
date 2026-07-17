package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.cultivation.CultivationTask;
import com.ocmptd.idlecultivator.game.image.ImageCacheService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.portrait.PortraitService;

import java.util.Optional;

public class StatusCommand implements Command {
    private final PlayerService playerService;
    private final CultivationService cultivationService;
    private final PortraitService portraitService;
    private final ImageCacheService imageCache;

    public StatusCommand(PlayerService playerService, CultivationService cultivationService,
                         PortraitService portraitService, ImageCacheService imageCache) {
        this.playerService = playerService;
        this.cultivationService = cultivationService;
        this.portraitService = portraitService;
        this.imageCache = imageCache;
    }

    @Override
    public String name() {
        return "状态";
    }

    @Override
    public String usage() {
        return "状态 —— 查看境界、属性与修炼进度";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();
        StringBuilder sb = new StringBuilder(playerService.describe(p));
        Optional<CultivationTask> task = cultivationService.activeTask(p.userId());
        if (task.isPresent()) {
            CultivationTask t = task.get();
            if (t.status() == CultivationTask.STATUS_READY || t.endTime() <= System.currentTimeMillis()) {
                sb.append("\n修炼已完成,可 ").append(ctx.prefix()).append("收获");
            } else {
                long remainMinutes = Math.max(0, (t.endTime() - System.currentTimeMillis()) / 60_000);
                sb.append("\n正在修炼")
                        .append(t.method() == null ? "" : "《" + t.method() + "》")
                        .append(",剩余约 ").append(remainMinutes).append(" 分钟");
            }
        }
        sb.append("\n").append(portraitService.describeAppearance(p));
        String appearancePrompt = portraitService.appearancePrompt(p);
        ImageCacheService.AsyncLookup image =
                imageCache.lookupOrGenerateAsync(appearancePrompt);
        switch (image.state()) {
            case HIT -> {
                ctx.addImage(image.path());
                sb.append("\n形象图片已生成(见下)");
            }
            case GENERATING -> sb.append("\n形象图片生成中,请稍后再看");
            case DISABLED -> {
            }
        }
        return sb.toString();
    }
}

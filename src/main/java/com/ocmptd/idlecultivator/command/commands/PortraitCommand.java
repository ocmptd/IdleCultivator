package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.image.ImageCacheService;
import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.portrait.PortraitService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * 展示角色/道具的图片描述词(AI 绘图 prompt),暂不实际生成图片。
 */
public class PortraitCommand implements Command {
    private final PlayerService playerService;
    private final PortraitService portraitService;
    private final ImageCacheService imageCache;

    public PortraitCommand(PlayerService playerService, PortraitService portraitService, ImageCacheService imageCache) {
        this.playerService = playerService;
        this.portraitService = portraitService;
        this.imageCache = imageCache;
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
            return itemPrompts(p);
        }
        String prompt = portraitService.imagePrompt(p);
        return portraitService.describeAppearance(p)
                + "\n\n=== 图片描述词(暂不生成图片) ===\n" + prompt
                + "\n缓存状态: " + cacheStatus(imageCache.getOrGenerate(prompt));
    }

    private String itemPrompts(Player p) {
        Map<String, Integer> items = Inventory.parse(p.inventory());
        if (items.isEmpty()) return "背包空空如也,暂无道具描述词。";
        StringBuilder sb = new StringBuilder("=== 道具图片描述词 ===");
        for (String name : items.keySet()) {
            String prompt = portraitService.itemImagePrompt(name);
            sb.append("\n【").append(name).append("】").append(prompt)
                    .append("\n缓存状态: ").append(cacheStatus(imageCache.getOrGenerate(prompt)));
        }
        return sb.toString();
    }

    private String cacheStatus(Optional<Path> cachedPath) {
        return cachedPath.map(path -> "命中,图片路径: " + path)
                .orElse("(暂无缓存图片,待接入 AI 生图)");
    }
}

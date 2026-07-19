package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;

/**
 * 修改称号,冷却 2 天。
 */
public class RenameCommand implements Command {
    public static final long COOLDOWN_MILLIS = 2L * 24 * 60 * 60 * 1000;
    public static final int MAX_LENGTH = 12;

    private final PlayerService playerService;

    public RenameCommand(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return "改称号";
    }

    @Override
    public String usage() {
        return "改称号 [新称号] —— 修改称号,每 2 天可改一次";
    }

    @Override
    public boolean inlineArgs() {
        return true;
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";
        Player p = opt.get();

        String newName = ctx.arg(0);
        if (newName == null || newName.isBlank()) {
            return "请输入新称号:" + ctx.prefix() + "改称号 [新称号]";
        }
        if (newName.length() > MAX_LENGTH) {
            return "称号过长,最多 " + MAX_LENGTH + " 个字。";
        }
        if (newName.equals(p.name())) {
            return "新称号与当前称号相同。";
        }
        long now = System.currentTimeMillis();
        long since = now - p.nameChangedAt();
        if (p.nameChangedAt() > 0 && since < COOLDOWN_MILLIS) {
            long remainHours = (COOLDOWN_MILLIS - since) / 3_600_000 + 1;
            return "称号修改冷却中,还需约 " + remainHours + " 小时(每 2 天可改一次)。";
        }
        String old = p.name();
        p.setName(newName);
        p.setNameChangedAt(now);
        playerService.save(p);
        return "称号修改成功!" + (old == null ? "" : "「" + old + "」→ ") + "「" + newName + "」(下次可修改时间:2 天后)";
    }
}

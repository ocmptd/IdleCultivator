package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CultivateCommand implements Command {
    private static final Pattern DURATION = Pattern.compile("^(\\d+)(h|H|小时|m|M|分钟|min)?$");

    private final PlayerService playerService;
    private final CultivationService cultivationService;

    public CultivateCommand(PlayerService playerService, CultivationService cultivationService) {
        this.playerService = playerService;
        this.cultivationService = cultivationService;
    }

    @Override
    public String name() {
        return "修炼";
    }

    @Override
    public String usage() {
        return "修炼 [功法名] [时长] —— 开始自动修炼,默认 30 分钟(如:修炼 紫府诀 4h)";
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";

        String method = null;
        int minutes = CultivationService.DEFAULT_MINUTES;
        for (String arg : ctx.args()) {
            Integer parsed = parseMinutes(arg);
            if (parsed != null) {
                minutes = parsed;
            } else if (method == null) {
                method = arg;
            }
        }
        return cultivationService.start(opt.get(), method, minutes);
    }

    private Integer parseMinutes(String arg) {
        Matcher m = DURATION.matcher(arg);
        if (!m.matches()) return null;
        String unit = m.group(2);
        if (unit == null) return null;
        int value = Integer.parseInt(m.group(1));
        return switch (unit.toLowerCase()) {
            case "h", "小时" -> value * 60;
            default -> value;
        };
    }
}

package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationMethod;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.SocialService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CultivateCommand implements Command {
    private static final Pattern DURATION = Pattern.compile("^(\\d+)(h|H|小时|m|M|分钟|min)?$");

    private final PlayerService playerService;
    private final CultivationService cultivationService;
    private final GroupService groupService;
    private final SocialService socialService;

    public CultivateCommand(PlayerService playerService, CultivationService cultivationService,
                           GroupService groupService, SocialService socialService) {
        this.playerService = playerService;
        this.cultivationService = cultivationService;
        this.groupService = groupService;
        this.socialService = socialService;
    }

    @Override
    public String name() {
        return "修炼";
    }

    @Override
    public String usage() {
        return "修炼 [功法名] [时长] —— 开始自动修炼,默认 18~28 分钟随机,最长 2 小时,时长越长收益越高(如:修炼 紫府诀 2h)";
    }

    @Override
    public boolean inlineArgs() {
        return true;
    }

    @Override
    public String execute(CommandContext ctx) {
        Optional<Player> opt = playerService.find(ctx.userId());
        if (opt.isEmpty()) return "道友尚未创建角色,请先 " + ctx.prefix() + "创建角色";

        CultivationMethod method = null;
        int minutes = -1;
        for (String arg : ctx.args()) {
            Integer parsed = parseMinutes(arg);
            if (parsed != null) {
                minutes = parsed;
            } else if (method == null) {
                method = CultivationMethod.fromLabel(arg);
                if (method == null) {
                    return "未知功法:" + arg + "\n" + CultivationMethod.describeAll();
                }
            }
        }
        // 计算社交效率倍率 = 群活跃度倍率 x 潮汐倍率 x 互助倍率 x 道侣倍率 x 社交惩罚倍率
        double groupMult = groupService.groupMultiplier(ctx.groupId());
        double socialMult = socialService.socialMultiplier(ctx.userId());
        double totalMult = groupMult * socialMult;
        // 方向修炼加成
        double directionBonus = opt.get().direction() == null ? 1.0 : opt.get().direction().cultivationBonus();
        double effectiveMult = totalMult * directionBonus;
        String result = cultivationService.start(opt.get(), method, minutes, totalMult);
        // 附加倍率提示
        if (effectiveMult > 1.0) {
            int pct = (int) Math.round((effectiveMult - 1.0) * 100);
            if (pct > 0) result += " (效率+" + pct + "%)";
        } else if (effectiveMult < 1.0) {
            int pct = (int) Math.round((1.0 - effectiveMult) * 100);
            if (pct > 0) result += " (效率-" + pct + "%)";
        }
        return result;
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

package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.CultivationDirection;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.player.Realm;

import java.util.Optional;

/**
 * 选择修炼方向（筑基期后），已选方向可花费灵石更换。
 */
public class ChooseDirectionCommand implements Command {
    public static final long CHANGE_COST = 500;

    private final PlayerService playerService;

    public ChooseDirectionCommand(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return "选择方向";
    }

    @Override
    public String usage() {
        return "选择方向 剑修/法修/医修 —— 筑基期后选择修炼方向（更换需500灵石）";
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

        if (p.realm().ordinal() < Realm.ZHU_JI.ordinal()) {
            return "选择修炼方向需筑基期及以上,道友当前为" + p.realm().label() + ",请先突破。";
        }

        String arg = ctx.arg(0);
        if (arg == null || arg.isBlank()) {
            if (p.direction() != null) {
                return "道友当前方向:" + p.direction().label() + "(" + p.direction().bonusDesc() + ")\n"
                        + "如需更换,输入 " + ctx.prefix() + "选择方向 剑修/法修/医修（花费" + CHANGE_COST + "灵石）\n\n"
                        + CultivationDirection.describeAll();
            }
            return "请选择修炼方向:\n" + CultivationDirection.describeAll()
                    + "\n\n输入 " + ctx.prefix() + "选择方向 剑修/法修/医修";
        }

        CultivationDirection dir = CultivationDirection.fromLabel(arg);
        if (dir == null) {
            return "未知方向:" + arg + "\n" + CultivationDirection.describeAll();
        }

        if (p.direction() == dir) {
            return "道友已是" + dir.label() + ",无需重复选择。";
        }

        // 首次选择免费，后续更换需花费灵石
        if (p.direction() != null) {
            if (p.spiritStones() < CHANGE_COST) {
                return "更换方向需" + CHANGE_COST + "灵石,道友当前仅有" + p.spiritStones() + "灵石。";
            }
            p.setSpiritStones(p.spiritStones() - CHANGE_COST);
        }

        CultivationDirection old = p.direction();
        p.setDirection(dir);
        playerService.save(p);

        String msg = "修炼方向已设定为「" + dir.label() + "」!" + dir.bonusDesc();
        if (old != null) {
            msg = "修炼方向已从「" + old.label() + "」更换为「" + dir.label() + "」(消耗" + CHANGE_COST + "灵石)!" + dir.bonusDesc();
        }
        msg += "\n可修炼专属功法,输入 " + ctx.prefix() + "功法 查看一览。";
        return msg;
    }
}

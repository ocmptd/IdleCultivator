package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

public class CreateRoleCommand implements Command {
    private final PlayerService playerService;

    public CreateRoleCommand(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public String name() {
        return "创建角色";
    }

    @Override
    public String usage() {
        return "创建角色 [男/女] [称号] —— 创建修仙角色";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (playerService.find(ctx.userId()).isPresent()) {
            return "道友已踏上修仙之路,无需重复创建。输入 " + ctx.prefix() + "状态 查看角色。";
        }
        String genderArg = ctx.arg(0);
        if (genderArg == null) {
            return "欢迎道友!请选择性别与初始称号:" + ctx.prefix() + "创建角色 男/女 [称号]";
        }
        Gender gender = Gender.fromLabel(genderArg);
        if (gender == null) {
            return "性别只可选「男」或「女」。";
        }
        String name = ctx.arg(1);
        Player p = playerService.create(ctx.userId(), ctx.groupId(), name, gender);
        String bonus = gender == Gender.MALE ? "体魄+5%(宜剑修)" : "神识+5%(宜法修)";
        return "角色创建成功!性别:" + gender.label() + "," + bonus
                + (p.name() != null ? ",称号:" + p.name() : "")
                + "\n输入 " + ctx.prefix() + "修炼 开始修仙之旅!";
    }
}

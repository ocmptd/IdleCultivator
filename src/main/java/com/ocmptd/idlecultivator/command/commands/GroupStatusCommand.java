package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.social.Beast;
import com.ocmptd.idlecultivator.game.social.BeastService;
import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.GroupStatus;
import com.ocmptd.idlecultivator.game.social.SectWarService;

import java.util.Optional;

/**
 * !群状态 —— 查看群活跃度、灵气潮汐、异兽、宗门排名。
 */
public class GroupStatusCommand implements Command {
    private final GroupService groupService;
    private final BeastService beastService;
    private final SectWarService sectWarService;

    public GroupStatusCommand(GroupService groupService, BeastService beastService, SectWarService sectWarService) {
        this.groupService = groupService;
        this.beastService = beastService;
        this.sectWarService = sectWarService;
    }

    @Override
    public String name() {
        return "群状态";
    }

    @Override
    public String usage() {
        return "群状态 —— 查看群活跃度、灵气潮汐与异兽信息";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (ctx.groupId() == null) return "此指令仅限群内使用。";
        GroupStatus gs = groupService.getGroupStatus(ctx.groupId());
        StringBuilder sb = new StringBuilder(groupService.describe(gs));
        // 异兽信息
        Optional<Beast> beast = beastService.currentBeast(ctx.groupId());
        if (beast.isPresent()) {
            sb.append("\n⚠ 异兽出没:").append(beast.get().name())
                    .append(" HP:").append(beast.get().hp()).append("/").append(beast.get().maxHp())
                    .append(" 输入 ").append(ctx.prefix()).append("击杀 攻击!");
        }
        // 宗门战排名
        int rank = sectWarService.currentRankOf(ctx.groupId());
        if (rank > 0) {
            sb.append("\n宗门战本周排名第 ").append(rank);
        }
        return sb.toString();
    }
}

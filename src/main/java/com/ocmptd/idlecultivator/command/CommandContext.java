package com.ocmptd.idlecultivator.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次指令调用的上下文。
 *
 * @param userId          发送者 ID
 * @param groupId         群 ID(私聊/控制台为 null)
 * @param args            指令参数(不含指令名)
 * @param prefix          当前配置的指令前缀(可为空),用于回复文案中提示其他指令
 * @param images          指令执行过程中附加的图片
 * @param mentionedUserIds 消息中 @ 提及的用户 ID 列表
 */
public record CommandContext(String userId, String groupId, List<String> args, String prefix,
                             List<Path> images, List<String> mentionedUserIds) {

    public CommandContext(String userId, String groupId, List<String> args, String prefix) {
        this(userId, groupId, args, prefix, new ArrayList<>(), List.of());
    }

    public CommandContext(String userId, String groupId, List<String> args, String prefix,
                         List<String> mentionedUserIds) {
        this(userId, groupId, args, prefix, new ArrayList<>(), mentionedUserIds);
    }

    public String arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }

    /** 获取第一个 @ 提及的用户 ID,无则返回 null。 */
    public String firstMentionedUserId() {
        return mentionedUserIds == null || mentionedUserIds.isEmpty() ? null : mentionedUserIds.get(0);
    }

    public void addImage(Path image) {
        images.add(image);
    }
}

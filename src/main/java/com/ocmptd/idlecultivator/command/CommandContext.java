package com.ocmptd.idlecultivator.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次指令调用的上下文。
 *
 * @param userId  发送者 ID
 * @param groupId 群 ID(私聊/控制台为 null)
 * @param args    指令参数(不含指令名)
 * @param prefix  当前配置的指令前缀(可为空),用于回复文案中提示其他指令
 * @param images  指令执行过程中附加的图片
 */
public record CommandContext(String userId, String groupId, List<String> args, String prefix,
                             List<Path> images) {

    public CommandContext(String userId, String groupId, List<String> args, String prefix) {
        this(userId, groupId, args, prefix, new ArrayList<>());
    }

    public String arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }

    public void addImage(Path image) {
        images.add(image);
    }
}

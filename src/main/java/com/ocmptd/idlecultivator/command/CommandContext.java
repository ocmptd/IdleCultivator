package com.ocmptd.idlecultivator.command;

import java.util.List;

/**
 * 一次指令调用的上下文。
 *
 * @param userId  发送者 ID
 * @param groupId 群 ID(私聊/控制台为 null)
 * @param args    指令参数(不含指令名)
 */
public record CommandContext(String userId, String groupId, List<String> args) {

    public String arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }
}

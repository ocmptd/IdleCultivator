package com.ocmptd.idlecultivator.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令路由:解析 "!指令 参数..." 并分发到已注册的 {@link Command}。
 * 兼容全角感叹号 "!"。
 */
public class CommandRouter {
    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(Command command) {
        commands.put(command.name(), command);
    }

    public Map<String, Command> commands() {
        return commands;
    }

    /**
     * 处理一条原始消息。非指令消息返回 null。
     */
    public String handle(String userId, String groupId, String rawMessage) {
        if (rawMessage == null) return null;
        String text = rawMessage.trim();
        if (text.startsWith("！")) text = "!" + text.substring(1);
        if (!text.startsWith("!")) return null;

        String[] parts = text.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return null;
        Command command = commands.get(parts[0]);
        if (command == null) {
            return "未知指令:" + parts[0] + ",输入 !帮助 查看指令列表";
        }
        List<String> args = Arrays.asList(parts).subList(1, parts.length);
        try {
            return command.execute(new CommandContext(userId, groupId, args));
        } catch (Exception e) {
            log.error("执行指令 {} 失败", parts[0], e);
            return "天机紊乱,指令执行失败,请稍后再试。";
        }
    }
}

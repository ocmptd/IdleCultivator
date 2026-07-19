package com.ocmptd.idlecultivator.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令路由:解析 "[前缀]指令 参数..." 并分发到已注册的 {@link Command}。
 * 前缀可配置,可为空(无前缀直接输入指令名)。前缀为 "!" 时兼容全角 "！"。
 */
public class CommandRouter {
    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final String prefix;

    public CommandRouter() {
        this("!");
    }

    public CommandRouter(String prefix) {
        this.prefix = prefix == null ? "" : prefix.trim();
    }

    public String prefix() {
        return prefix;
    }

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
        CommandReply reply = handleWithReply(userId, groupId, rawMessage);
        return reply == null ? null : reply.text();
    }

    public CommandReply handleWithReply(String userId, String groupId, String rawMessage) {
        return handleWithReply(userId, groupId, rawMessage, List.of());
    }

    public CommandReply handleWithReply(String userId, String groupId, String rawMessage, List<String> mentionedUserIds) {
        if (rawMessage == null) return null;
        String text = rawMessage.trim();
        if (!prefix.isEmpty()) {
            if (prefix.equals("!") && text.startsWith("！")) text = "!" + text.substring(1);
            if (!text.startsWith(prefix)) return null;
            text = text.substring(prefix.length()).trim();
        }

        String[] parts = text.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return null;

        String commandName = parts[0];
        Command command = commands.get(commandName);
        List<String> args;
        if (command != null) {
            args = Arrays.asList(parts).subList(1, parts.length);
        } else {
            // 无空格粘连参数识别:如 "换发型默认" 识别为 "换发型" + "默认"
            commandName = longestInlinePrefix(text);
            command = commandName == null ? null : commands.get(commandName);
            if (command != null) {
                String rest = text.substring(commandName.length()).trim();
                args = rest.isEmpty() ? List.of() : Arrays.asList(rest.split("\\s+"));
            } else {
                args = List.of();
            }
        }

        if (command == null) {
            // 无前缀模式下,未匹配的普通聊天消息不回复,避免刷屏
            if (prefix.isEmpty()) return null;
            return new CommandReply(
                    "未知指令:" + parts[0] + ",输入 " + prefix + "帮助 查看指令列表",
                    new ArrayList<>());
        }
        CommandContext context = new CommandContext(userId, groupId, args, prefix, mentionedUserIds);
        try {
            return new CommandReply(command.execute(context), context.images());
        } catch (Exception e) {
            log.error("执行指令 {} 失败", commandName, e);
            return new CommandReply("天机紊乱,指令执行失败,请稍后再试。", new ArrayList<>());
        }
    }

    /**
     * 查找作为 text 前缀、且开启了 {@link Command#inlineArgs()} 的最长指令名,
     * 用于无空格粘连参数识别(如 "换发型默认")。仅对显式开启的指令生效,
     * 避免无前缀模式下把普通聊天误判为指令。
     */
    private String longestInlinePrefix(String text) {
        String matched = null;
        for (Command command : commands.values()) {
            if (!command.inlineArgs()) continue;
            String name = command.name();
            if (text.length() > name.length() && text.startsWith(name)
                    && (matched == null || name.length() > matched.length())) {
                matched = name;
            }
        }
        return matched;
    }
}

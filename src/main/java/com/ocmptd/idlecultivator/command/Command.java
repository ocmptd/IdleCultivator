package com.ocmptd.idlecultivator.command;

/**
 * 游戏指令。实现类注册到 {@link CommandRouter}。
 */
public interface Command {
    /** 指令名,如 "修炼"(不含前缀 "!") */
    String name();

    /** 用法说明,用于 !帮助 */
    String usage();

    /** 执行指令并返回回复文本 */
    String execute(CommandContext ctx);
}

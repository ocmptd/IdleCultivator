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

    /**
     * 是否支持无空格粘连参数(如 "换发型默认" 识别为 "换发型" + "默认")。
     * 默认关闭,避免无前缀模式下把普通聊天误判为指令。
     */
    default boolean inlineArgs() {
        return false;
    }
}

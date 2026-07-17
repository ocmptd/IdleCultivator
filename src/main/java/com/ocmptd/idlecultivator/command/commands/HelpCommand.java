package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.command.CommandRouter;

public class HelpCommand implements Command {
    private final CommandRouter router;

    public HelpCommand(CommandRouter router) {
        this.router = router;
    }

    @Override
    public String name() {
        return "帮助";
    }

    @Override
    public String usage() {
        return "帮助 —— 查看指令列表";
    }

    @Override
    public String execute(CommandContext ctx) {
        StringBuilder sb = new StringBuilder("=== 修仙指令一览 ===");
        for (Command c : router.commands().values()) {
            sb.append("\n").append(router.prefix()).append(c.usage());
        }
        return sb.toString();
    }
}

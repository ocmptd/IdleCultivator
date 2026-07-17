package com.ocmptd.idlecultivator.command.commands;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.game.cultivation.CultivationMethod;

public class MethodsCommand implements Command {

    @Override
    public String name() {
        return "功法";
    }

    @Override
    public String usage() {
        return "功法 —— 查看可修炼的功法及收益倍率";
    }

    @Override
    public String execute(CommandContext ctx) {
        return CultivationMethod.describeAll();
    }
}

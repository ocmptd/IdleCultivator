package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.command.CommandRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRouterTest {

    private CommandRouter routerWithEcho() {
        return routerWithEcho("!");
    }

    private CommandRouter routerWithEcho(String prefix) {
        CommandRouter router = new CommandRouter(prefix);
        router.register(new Command() {
            @Override
            public String name() {
                return "回声";
            }

            @Override
            public String usage() {
                return "!回声 [内容]";
            }

            @Override
            public String execute(CommandContext ctx) {
                return String.join(",", ctx.args());
            }
        });
        return router;
    }

    @Test
    void nonCommandMessageReturnsNull() {
        assertNull(routerWithEcho().handle("u", "g", "普通聊天消息"));
    }

    @Test
    void dispatchesToCommandWithArgs() {
        assertEquals("a,b", routerWithEcho().handle("u", "g", "!回声 a b"));
    }

    @Test
    void supportsFullWidthExclamation() {
        assertEquals("x", routerWithEcho().handle("u", "g", "！回声 x"));
    }

    @Test
    void unknownCommandGivesHint() {
        assertTrue(routerWithEcho().handle("u", "g", "!不存在").contains("未知指令"));
    }

    @Test
    void customPrefixDispatches() {
        CommandRouter router = routerWithEcho("#");
        assertEquals("a", router.handle("u", "g", "#回声 a"));
        assertNull(router.handle("u", "g", "!回声 a"));
    }

    @Test
    void emptyPrefixDispatchesBareCommand() {
        CommandRouter router = routerWithEcho("");
        assertEquals("a", router.handle("u", "g", "回声 a"));
        // 未匹配的普通聊天消息不回复
        assertNull(router.handle("u", "g", "随便聊聊"));
    }
}

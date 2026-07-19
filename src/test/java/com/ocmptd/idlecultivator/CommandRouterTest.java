package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.command.Command;
import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.command.CommandRouter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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

    @Test
    void inlineArgsCommandSplitsWithoutSpace() {
        CommandRouter router = new CommandRouter("");
        router.register(new Command() {
            @Override
            public String name() {
                return "换发型";
            }

            @Override
            public String usage() {
                return "换发型 [名称]";
            }

            @Override
            public boolean inlineArgs() {
                return true;
            }

            @Override
            public String execute(CommandContext ctx) {
                return String.join(",", ctx.args());
            }
        });
        // 无空格粘连:换发型默认 -> 换发型 + 默认
        assertEquals("默认", router.handle("u", "g", "换发型默认"));
        // 带空格仍正常
        assertEquals("素髻", router.handle("u", "g", "换发型 素髻"));
        // 无参数
        assertEquals("", router.handle("u", "g", "换发型"));
    }

    @Test
    void nonInlineCommandDoesNotPrefixMatch() {
        // 默认不开启 inlineArgs 的指令不会被前缀匹配,普通聊天不误触发
        CommandRouter router = routerWithEcho("");
        assertNull(router.handle("u", "g", "回声不错啊"));
    }

    @Test
    void commandReplyExposesAttachedImages() {
        Path image = Path.of("image-cache/portrait.png");
        CommandRouter router = new CommandRouter("!");
        router.register(new Command() {
            @Override
            public String name() {
                return "图片";
            }

            @Override
            public String usage() {
                return "!图片";
            }

            @Override
            public String execute(CommandContext ctx) {
                ctx.addImage(image);
                return "图片已就绪";
            }
        });

        var reply = router.handleWithReply("u", "g", "!图片");

        assertEquals("图片已就绪", reply.text());
        assertEquals(java.util.List.of(image), reply.images());
    }
}

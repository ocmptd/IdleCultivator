package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.bot.BotService;
import io.github.kloping.qqbot.entities.ex.At;
import io.github.kloping.qqbot.entities.ex.PlainText;
import io.github.kloping.qqbot.entities.ex.msg.MessageChain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class BotServiceTest {

    @Test
    void extractsPlainTextWithoutBrackets() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("!帮助"));
        assertEquals("!帮助", BotService.extractText(chain));
    }

    @Test
    void stripsAtTagsFromText() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("<@9E223402C57CAE80D1A71321C4A7331F> !帮助"));
        assertEquals("!帮助", BotService.extractText(chain));
    }

    @Test
    void detectsAtPresence() {
        MessageChain withTextAt = new MessageChain();
        withTextAt.append(new PlainText("<@ABC123> !状态"));
        assertTrue(BotService.containsAt(withTextAt));

        MessageChain withAtElement = new MessageChain();
        withAtElement.append(new At("123"));
        withAtElement.append(new PlainText("!状态"));
        assertTrue(BotService.containsAt(withAtElement));

        MessageChain plain = new MessageChain();
        plain.append(new PlainText("!状态"));
        assertFalse(BotService.containsAt(plain));
    }

    @Test
    void extractsMentionedUserIdsFromText() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("<@ABC123> !互助"));
        List<String> ids = BotService.extractMentionedUserIds(chain);
        assertEquals(1, ids.size());
        assertEquals("ABC123", ids.get(0));
    }

    @Test
    void extractsMultipleMentionedUserIds() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("<@AAA> <@BBB> !道侣"));
        List<String> ids = BotService.extractMentionedUserIds(chain);
        assertEquals(2, ids.size());
        assertEquals("AAA", ids.get(0));
        assertEquals("BBB", ids.get(1));
    }

    @Test
    void extractsMentionedUserIdsFromQqbotFormat() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("<qqbot-at-user id=\"XYZ789\" /> !互助"));
        List<String> ids = BotService.extractMentionedUserIds(chain);
        assertEquals(1, ids.size());
        assertEquals("XYZ789", ids.get(0));
    }

    @Test
    void noMentionedUserIdsReturnsEmpty() {
        MessageChain chain = new MessageChain();
        chain.append(new PlainText("!状态"));
        List<String> ids = BotService.extractMentionedUserIds(chain);
        assertTrue(ids.isEmpty());
    }
}

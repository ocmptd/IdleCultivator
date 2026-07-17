package com.ocmptd.idlecultivator.bot;

import com.ocmptd.idlecultivator.command.CommandRouter;
import com.ocmptd.idlecultivator.config.BotConfig;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.qqbot.api.Intents;
import io.github.kloping.qqbot.api.v2.FriendMessageEvent;
import io.github.kloping.qqbot.api.v2.GroupMessageEvent;
import io.github.kloping.qqbot.impl.ListenerHost;
import io.github.kloping.qqbot.impl.ListenerHost.EventReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * qqpd-bot-java 接入层:启动 Starter 并将群/好友消息转发给 {@link CommandRouter}。
 */
public class BotService {
    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    private final BotConfig config;
    private final CommandRouter router;
    private Starter starter;

    public BotService(BotConfig config, CommandRouter router) {
        this.config = config;
        this.router = router;
    }

    public void start() {
        starter = new Starter(config.appId(), config.token(), config.secret());
        starter.getConfig().setCode(Intents.PUBLIC_INTENTS.and(Intents.GROUP_INTENTS));
        if (config.sandbox()) starter.getConfig().sandbox();
        starter.run();
        starter.registerListenerHost(new ListenerHost() {
            @EventReceiver
            public void onGroupMessage(GroupMessageEvent event) {
                String content = event.getMessage().toString().trim();
                String reply = router.handle(event.getSender().getOpenid(), event.getSubject().getOpenid(), content);
                if (reply != null) event.sendMessage(reply);
            }

            @EventReceiver
            public void onFriendMessage(FriendMessageEvent event) {
                String content = event.getMessage().toString().trim();
                String reply = router.handle(event.getSender().getOpenid(), null, content);
                if (reply != null) event.sendMessage(reply);
            }
        });
        log.info("QQ 机器人已启动 (appid={})", config.appId());
    }
}

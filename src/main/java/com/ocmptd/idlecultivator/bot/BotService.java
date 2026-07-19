package com.ocmptd.idlecultivator.bot;

import com.ocmptd.idlecultivator.command.CommandRouter;
import com.ocmptd.idlecultivator.command.CommandReply;
import com.ocmptd.idlecultivator.config.BotConfig;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.cultivation.CultivationTask;
import com.ocmptd.idlecultivator.game.social.Beast;
import com.ocmptd.idlecultivator.game.social.BeastService;
import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.GroupStatus;
import com.ocmptd.idlecultivator.game.social.SocialService;
import com.ocmptd.idlecultivator.storage.NoticeRepository;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.qqbot.api.Intents;
import io.github.kloping.qqbot.api.SendAble;
import io.github.kloping.qqbot.api.v2.FriendMessageEvent;
import io.github.kloping.qqbot.api.v2.GroupMessageEvent;
import io.github.kloping.qqbot.entities.ex.At;
import io.github.kloping.qqbot.entities.ex.Image;
import io.github.kloping.qqbot.entities.ex.PlainText;
import io.github.kloping.qqbot.entities.ex.msg.MessageChain;
import io.github.kloping.qqbot.entities.qqpd.v2.Group;
import io.github.kloping.qqbot.impl.ListenerHost;
import io.github.kloping.qqbot.impl.ListenerHost.EventReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * qqpd-bot-java 接入层:启动 Starter 并将群/好友消息转发给 {@link CommandRouter}。
 */
public class BotService {
    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    /** 消息文本中的 at 标记,如 <@xxx> 或 <qqbot-at-user id="xxx" /> */
    private static final Pattern AT_TAG = Pattern.compile("<@!?[0-9A-Za-z_-]+>|<qqbot-at-user\\s+id=\"[^\"]*\"\\s*/>");

    private final BotConfig config;
    private final CommandRouter router;
    private final CultivationService cultivationService;
    private final NoticeRepository noticeRepository;
    private final GroupService groupService;
    private final SocialService socialService;
    private final BeastService beastService;
    /** 群 openid → 最近一次消息的 Group,用于修炼结算主动推送 */
    private final Map<String, Group> knownGroups = new ConcurrentHashMap<>();
    private Starter starter;

    public BotService(BotConfig config, CommandRouter router, CultivationService cultivationService,
                      NoticeRepository noticeRepository, GroupService groupService,
                      SocialService socialService, BeastService beastService) {
        this.config = config;
        this.router = router;
        this.cultivationService = cultivationService;
        this.noticeRepository = noticeRepository;
        this.groupService = groupService;
        this.socialService = socialService;
        this.beastService = beastService;
    }

    public void start() {
        starter = new Starter(config.appId(), config.token(), config.secret());
        starter.getConfig().setCode(Intents.PUBLIC_INTENTS.and(Intents.GROUP_INTENTS));
        if (config.sandbox()) starter.getConfig().sandbox();
        starter.run();
        starter.registerListenerHost(new ListenerHost() {
            @EventReceiver
            public void onGroupMessage(GroupMessageEvent event) {
                String groupId = event.getSubject().getOpenid();
                String userId = event.getSender().getOpenid();
                knownGroups.put(groupId, event.getSubject());
                // 记录群消息(活跃度统计)
                groupService.recordMessage(groupId);
                // 更新玩家活跃时间(社交惩罚)
                socialService.updateActivity(userId);
                // 尝试生成异兽
                GroupStatus gs = groupService.getGroupStatus(groupId);
                beastService.trySpawn(groupId, gs.activityLevel()).ifPresent(beast ->
                        notifyBeastSpawn(groupId, beast));
                if (config.requireAt() && !containsAt(event.getMessage())) return;
                String content = extractText(event.getMessage());
                List<String> mentionedIds = extractMentionedUserIds(event.getMessage());
                CommandReply commandReply =
                        router.handleWithReply(userId, groupId, content, mentionedIds);
                String reply = commandReply == null ? null : commandReply.text();
                reply = withPendingNotices(event.getSender().getOpenid(), reply);
                List<Path> images = commandReply == null ? List.of() : commandReply.images();
                log.debug("群消息 content={} reply={} images={}", content, reply != null, images.size());
                MessageChain chain = groupReply(event.getSender().getOpenid(), reply, images);
                if (chain != null) {
                    event.sendMessage(chain);
                }
            }

            @EventReceiver
            public void onFriendMessage(FriendMessageEvent event) {
                String content = extractText(event.getMessage());
                socialService.updateActivity(event.getSender().getOpenid());
                CommandReply commandReply =
                        router.handleWithReply(event.getSender().getOpenid(), null, content);
                String reply = commandReply == null ? null : commandReply.text();
                reply = withPendingNotices(event.getSender().getOpenid(), reply);
                List<Path> images = commandReply == null ? List.of() : commandReply.images();
                MessageChain chain = friendReply(reply, images);
                if (chain != null) event.sendMessage(chain);
            }
        });
        cultivationService.setNotifier(this::pushCultivationMessage);
        beastService.setSpawnNotifier((gid, beast) -> {
            notifyBeastSpawn(gid, beast);
            return true;
        });
        log.info("QQ 机器人已启动 (appid={}, 指令前缀=\"{}\", 仅@触发={})",
                config.appId(), router.prefix(), config.requireAt());
    }

    /**
     * 把玩家的待送达结算消息(主动推送失败时暂存)拼到本次回复前面,
     * 利用 QQ 被动消息 5 分钟时效:玩家一发消息就先补发之前的结算信息。
     */
    private String withPendingNotices(String userId, String reply) {
        var notices = noticeRepository.drain(userId);
        if (notices.isEmpty()) return reply;
        String pending = "【修炼结算】" + String.join("\n【修炼结算】", notices);
        return reply == null ? pending : pending + "\n" + reply;
    }

    private MessageChain groupReply(String userId, String text, List<Path> images) {
        MessageChain chain = new MessageChain();
        if (text != null && !text.isEmpty()) chain.text("\n" + text);
        appendImages(chain, images);
        // 不再附带 @ 发送者:仅当既无文本也无图片时才跳过发送(空消息)
        return chain.isEmpty() ? null : chain;
    }

    private MessageChain friendReply(String text, List<Path> images) {
        MessageChain chain = new MessageChain();
        if (text != null && !text.isEmpty()) chain.text(text);
        appendImages(chain, images);
        return chain.isEmpty() ? null : chain;
    }

    private void appendImages(MessageChain chain, List<Path> images) {
        for (Path imagePath : images) {
            try {
                chain.append(new Image(
                        Files.readAllBytes(imagePath),
                        imagePath.getFileName().toString()));
            } catch (IOException e) {
                log.warn("读取图片失败,跳过发送: {}", imagePath, e);
            }
        }
    }

    /** 修炼结算/到期提醒推送到任务所在群,返回是否推送成功(失败时由修炼系统自动结算)。 */
    private boolean pushCultivationMessage(CultivationTask task, String message) {
        Group group = task.groupId() == null ? null : knownGroups.get(task.groupId());
        if (group == null) {
            log.info("[推送不可用,将自动结算] {}: {}", task.userId(), message);
            return false;
        }
        try {
            group.send(message);
            return true;
        } catch (Exception e) {
            log.error("推送修炼消息到群 {} 失败", task.groupId(), e);
            return false;
        }
    }

    /** 异兽生成通知推送到群。 */
    private void notifyBeastSpawn(String groupId, Beast beast) {
        Group group = knownGroups.get(groupId);
        if (group == null) return;
        try {
            group.send("异兽入侵!" + beast.name() + " 出现在本群(HP:" + beast.hp() + ")\n"
                    + "输入 !击杀 攻击异兽,击杀者可获得修为与灵石奖励!");
        } catch (Exception e) {
            log.error("推送异兽入侵通知到群 {} 失败", groupId, e);
        }
    }

    /**
     * 从消息链提取纯文本(拼接 PlainText 片段并去除 at 标记)。
     * 注意:MessageChain.toString() 返回 List.toString() 形式(带 [] 包裹),不可直接用于指令解析。
     */
    public static String extractText(MessageChain chain) {
        if (chain == null) return "";
        StringBuilder sb = new StringBuilder();
        for (SendAble element : chain) {
            if (element instanceof PlainText text) {
                sb.append(text.getText());
            }
        }
        return AT_TAG.matcher(sb.toString()).replaceAll(" ").trim();
    }

    public static boolean containsAt(MessageChain chain) {
        if (chain == null) return false;
        StringBuilder sb = new StringBuilder();
        for (SendAble element : chain) {
            if (element instanceof At) return true;
            if (element instanceof PlainText text) sb.append(text.getText());
        }
        return AT_TAG.matcher(sb).find();
    }

    /** 从消息链提取 @ 提及的用户 ID 列表。 */
    public static List<String> extractMentionedUserIds(MessageChain chain) {
        List<String> ids = new ArrayList<>();
        if (chain == null) return ids;
        StringBuilder sb = new StringBuilder();
        for (SendAble element : chain) {
            if (element instanceof PlainText text) sb.append(text.getText());
        }
        String text = sb.toString();
        Matcher m = Pattern.compile("<@!?([0-9A-Za-z_-]+)>").matcher(text);
        while (m.find()) ids.add(m.group(1));
        m = Pattern.compile("<qqbot-at-user\\s+id=\"([^\"]*)\"\\s*/>").matcher(text);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }
}

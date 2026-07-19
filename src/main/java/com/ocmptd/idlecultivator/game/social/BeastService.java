package com.ocmptd.idlecultivator.game.social;

import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.BeastRepository;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 异兽入侵服务:在活跃群随机生成妖兽,玩家可用 !击杀 攻击。
 * <p>
 * 妖兽类型(按群活跃度等级):
 * - 低阶妖兽:HP 100,奖励 50 修为 + 25 灵石
 * - 中阶妖兽:HP 300,奖励 150 修为 + 75 灵石
 * - 高阶妖兽:HP 600,奖励 400 修为 + 200 灵石
 * <p>
 * 攻击伤害 = 玩家等级 × 10 + 随机(1~20)。
 * 妖兽存在 1 小时后自动消失。
 */
public class BeastService {
    private static final Logger log = LoggerFactory.getLogger(BeastService.class);

    private static final long BEAST_LIFETIME_MS = 60 * 60_000L; // 1 小时
    private static final double SPAWN_CHANCE = 0.05; // 5% 概率在消息时生成

    private final BeastRepository repository;
    private final PlayerService playerService;

    @Setter
    private volatile BeastSpawnNotifier spawnNotifier = (groupId, beast) -> {
        log.info("[异兽入侵] 群 {} 出现 {}", groupId, beast.name());
        return false;
    };

    @FunctionalInterface
    public interface BeastSpawnNotifier {
        boolean notify(String groupId, Beast beast);
    }

    public BeastService(BeastRepository repository, PlayerService playerService) {
        this.repository = repository;
        this.playerService = playerService;
    }

    /** 异兽类型定义。 */
    private enum BeastType {
        LOW("低阶妖兽·赤鬃狼", 100, 50, 25),
        MID("中阶妖兽·玄冰蟒", 300, 150, 75),
        HIGH("高阶妖兽·九幽蛟", 600, 400, 200);

        final String name;
        final int hp;
        final long expReward;
        final long stoneReward;

        BeastType(String name, int hp, long expReward, long stoneReward) {
            this.name = name;
            this.hp = hp;
            this.expReward = expReward;
            this.stoneReward = stoneReward;
        }
    }

    /** 根据群活跃度选择妖兽类型。 */
    private BeastType pickBeastType(int activityLevel) {
        return switch (activityLevel) {
            case 0, 1 -> BeastType.LOW;
            case 2 -> ThreadLocalRandom.current().nextDouble() < 0.5 ? BeastType.LOW : BeastType.MID;
            default -> ThreadLocalRandom.current().nextDouble() < 0.4 ? BeastType.MID : BeastType.HIGH;
        };
    }

    /**
     * 尝试在群内生成异兽(由调度器或消息事件调用)。
     * @return 生成的异兽,如已有异兽或概率未命中则返回 empty
     */
    public Optional<Beast> trySpawn(String groupId, int activityLevel) {
        if (groupId == null) return Optional.empty();
        // 已有异兽则不再生成
        if (repository.findByGroupId(groupId).isPresent()) return Optional.empty();
        // 活跃度低时不生成
        if (activityLevel <= 0) return Optional.empty();
        // 概率检查
        if (ThreadLocalRandom.current().nextDouble() > SPAWN_CHANCE) return Optional.empty();

        BeastType type = pickBeastType(activityLevel);
        long now = System.currentTimeMillis();
        Beast beast = new Beast(groupId, type.name, type.hp, type.hp, type.expReward, type.stoneReward, now);
        repository.save(beast);
        log.info("异兽入侵:群 {} 出现 {} (HP={})", groupId, beast.name(), beast.hp());
        spawnNotifier.notify(groupId, beast);
        return Optional.of(beast);
    }

    /**
     * 攻击异兽(!击杀)。
     * 伤害 = 玩家等级 × 10 + 随机(1~20)。
     * @return 战斗结果描述
     */
    public String attack(String userId, String groupId) {
        Optional<Player> playerOpt = playerService.find(userId);
        if (playerOpt.isEmpty()) return "道友尚未创建角色,请先创建。";
        if (groupId == null) return "异兽仅出现在群中,请在群内使用此指令。";

        Optional<Beast> beastOpt = repository.findByGroupId(groupId);
        if (beastOpt.isEmpty()) return "本群暂无异兽出没。";

        Beast beast = beastOpt.get();
        // 检查是否超时
        if (System.currentTimeMillis() - beast.spawnTime() > BEAST_LIFETIME_MS) {
            repository.delete(groupId);
            return "异兽已自行离去。";
        }

        Player player = playerOpt.get();
        int damage = player.level() * 10 + ThreadLocalRandom.current().nextInt(1, 21);
        beast.setHp(Math.max(0, beast.hp() - damage));
        repository.save(beast);

        StringBuilder sb = new StringBuilder();
        sb.append("道友[").append(player.name() == null ? userId : player.name())
                .append("]对").append(beast.name()).append("造成 ").append(damage).append(" 点伤害!");

        if (beast.isDead()) {
            // 击杀奖励
            long exp = beast.expReward();
            long stones = beast.stoneReward();
            player.addSpiritStones(stones);
            String levelTip = playerService.gainExp(player, exp);
            sb.append("\n道友成功击杀").append(beast.name()).append("!获得 +").append(exp)
                    .append(" 修为,+").append(stones).append(" 灵石").append(levelTip);
            repository.delete(groupId);
            log.info("异兽被击杀:群 {} killer={} beast={} +{}修为 +{}灵石",
                    groupId, userId, beast.name(), exp, stones);
        } else {
            sb.append("\n").append(beast.name()).append("剩余 HP:").append(beast.hp()).append("/").append(beast.maxHp());
        }
        return sb.toString();
    }

    /** 获取群内当前异兽(如有)。 */
    public Optional<Beast> currentBeast(String groupId) {
        if (groupId == null) return Optional.empty();
        Optional<Beast> opt = repository.findByGroupId(groupId);
        if (opt.isPresent() && System.currentTimeMillis() - opt.get().spawnTime() > BEAST_LIFETIME_MS) {
            repository.delete(groupId);
            return Optional.empty();
        }
        return opt;
    }

    /** 清理超时异兽(由调度器调用,实际由 currentBeast 查询时自动清理)。 */
    public void cleanExpiredBeasts() {
        log.debug("清理超时异兽检查完成");
    }
}

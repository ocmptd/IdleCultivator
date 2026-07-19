package com.ocmptd.idlecultivator.game.social;

import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.SocialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 社交服务:互助、道侣、社交惩罚。
 * <p>
 * 互助(!互助 @某人):双方效率+20%/1h,最多叠3次(总+60%)。
 * 道侣(!道侣 @某人):永久效率+10%,仅限一名道侣。
 * 社交惩罚:超过24h未发言效率×0.9,48h×0.8,72h+×0.5,发消息后恢复。
 */
public class SocialService {
    private static final Logger log = LoggerFactory.getLogger(SocialService.class);

    /** 互助加成每次+20%,最多叠3次 */
    public static final double HELP_BONUS_PER_STACK = 0.2;
    public static final int MAX_HELP_STACKS = 3;
    /** 互助持续时间(1 小时) */
    public static final long HELP_DURATION_MS = 60 * 60_000L;
    /** 道侣加成+10% */
    public static final double DAO_COMPANION_BONUS = 0.1;

    private final SocialRepository repository;
    private final PlayerService playerService;

    public SocialService(SocialRepository repository, PlayerService playerService) {
        this.repository = repository;
        this.playerService = playerService;
    }

    // ==================== 互助 ====================

    /**
     * 请求互助:双方获得+20%效率加成(1小时)。
     * @return 结果描述文案
     */
    public String requestHelp(String userId, String targetId, String groupId) {
        if (targetId == null || targetId.isEmpty()) {
            return "请 @ 指定要互助的道友(如:!互助 @某人)";
        }
        if (targetId.equals(userId)) {
            return "道友不可与自己互助。";
        }
        Optional<Player> targetOpt = playerService.find(targetId);
        if (targetOpt.isEmpty()) {
            return "对方尚未创建角色,无法互助。";
        }
        long now = System.currentTimeMillis();
        if (repository.hasActiveHelp(userId, targetId, now)) {
            return "道友已与对方处于互助状态,无需重复发起。";
        }
        int currentStacks = repository.countActiveHelps(userId, now);
        if (currentStacks >= MAX_HELP_STACKS) {
            return "道友的互助加成已满 " + MAX_HELP_STACKS + " 次,请等待现有互助结束后再发起。";
        }
        long expireTime = now + HELP_DURATION_MS;
        repository.addMutualHelp(userId, targetId, groupId, expireTime);
        long remainMin = HELP_DURATION_MS / 60_000;
        int newStacks = currentStacks + 1;
        double bonus = newStacks * HELP_BONUS_PER_STACK * 100;
        return "道友与对方结成互助!双方修炼效率+" + (int) bonus + "%,持续约 " + remainMin
                + " 分钟(当前互助层数:" + newStacks + "/" + MAX_HELP_STACKS + ")";
    }

    /** 获取玩家当前互助效率倍率(1.0 + 0.2 × stacks,最多1.6)。 */
    public double helpMultiplier(String userId) {
        int stacks = repository.countActiveHelps(userId, System.currentTimeMillis());
        return 1.0 + HELP_BONUS_PER_STACK * Math.min(stacks, MAX_HELP_STACKS);
    }

    /** 获取玩家当前互助详情列表(用于状态展示)。 */
    public List<String> helpDetails(String userId) {
        return repository.activeHelpDetails(userId, System.currentTimeMillis());
    }

    /** 清理过期互助记录(由调度器调用)。 */
    public void cleanExpiredHelps() {
        repository.cleanExpiredHelps(System.currentTimeMillis());
    }

    // ==================== 道侣 ====================

    /**
     * 结为道侣:双方永久获得+10%效率加成。
     * @return 结果描述文案
     */
    public String establishDaoCompanion(String userId, String targetId, String groupId) {
        if (targetId == null || targetId.isEmpty()) {
            return "请 @ 指定要结为道侣的对象(如:!道侣 @某人)";
        }
        if (targetId.equals(userId)) {
            return "道友不可与自己结为道侣。";
        }
        Optional<Player> targetOpt = playerService.find(targetId);
        if (targetOpt.isEmpty()) {
            return "对方尚未创建角色,无法结为道侣。";
        }
        Optional<String> myPartner = repository.findPartnerId(userId);
        if (myPartner.isPresent()) {
            if (myPartner.get().equals(targetId)) {
                return "道友已与对方是道侣关系。";
            }
            return "道友已有道侣,需先解除方可重新结侣(输入: !道侣 解除)";
        }
        Optional<String> theirPartner = repository.findPartnerId(targetId);
        if (theirPartner.isPresent()) {
            return "对方已有道侣,无法结为道侣。";
        }
        long now = System.currentTimeMillis();
        repository.addDaoCompanion(userId, targetId, groupId, now);
        repository.addDaoCompanion(targetId, userId, groupId, now);
        log.info("道侣关系建立:{} ↔ {}", userId, targetId);
        return "道友与对方结为道侣!双方修炼效率永久+10%";
    }

    /** 解除道侣关系。 */
    public String breakDaoCompanion(String userId) {
        Optional<String> partner = repository.findPartnerId(userId);
        if (partner.isEmpty()) {
            return "道友当前没有道侣。";
        }
        repository.removeDaoCompanion(userId);
        repository.removeDaoCompanion(partner.get());
        return "道友已解除道侣关系。";
    }

    /** 查询道侣 ID。 */
    public Optional<String> findPartnerId(String userId) {
        return repository.findPartnerId(userId);
    }

    /** 道侣效率倍率(有道侣则1.1,否则1.0)。 */
    public double daoCompanionMultiplier(String userId) {
        return repository.findPartnerId(userId).isPresent() ? 1.0 + DAO_COMPANION_BONUS : 1.0;
    }

    // ==================== 社交惩罚 ====================

    /**
     * 社交惩罚倍率:长时间不发言导致修炼效率下降。
     * - 不足24h → 1.0(无惩罚)
     * - 24~48h → 0.9
     * - 48~72h → 0.8
     * - 72h+ → 0.5
     */
    public double inactivityMultiplier(String userId) {
        Optional<Player> opt = playerService.find(userId);
        if (opt.isEmpty()) return 1.0;
        long lastActive = opt.get().lastActiveTime();
        if (lastActive == 0) return 1.0; // 从未记录过活跃时间,不惩罚
        long hoursInactive = (System.currentTimeMillis() - lastActive) / (60 * 60_000L);
        if (hoursInactive < 24) return 1.0;
        if (hoursInactive < 48) return 0.9;
        if (hoursInactive < 72) return 0.8;
        return 0.5;
    }

    /** 更新玩家活跃时间(收到消息时调用)。 */
    public void updateActivity(String userId) {
        Optional<Player> opt = playerService.find(userId);
        if (opt.isPresent()) {
            Player p = opt.get();
            p.setLastActiveTime(System.currentTimeMillis());
            playerService.save(p);
        }
    }

    /**
     * 计算玩家的综合社交效率倍率 = 互助倍率 × 道侣倍率 × 社交惩罚倍率。
     */
    public double socialMultiplier(String userId) {
        return helpMultiplier(userId) * daoCompanionMultiplier(userId) * inactivityMultiplier(userId);
    }
}

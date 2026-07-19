package com.ocmptd.idlecultivator.game.cultivation;

import com.ocmptd.idlecultivator.game.player.LevelTable;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import com.ocmptd.idlecultivator.storage.NoticeRepository;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 修炼系统(最长 2 小时,时长越长修为/灵石越多且每小时效率越高):
 * - 修为 = 分钟数 × (1 + 分钟数/120) × (100/60) × 功法倍率 × 境界压制;灵石 = 修为÷2
 *   (约 30m→62 / 1h→150 / 2h→400;日均约 5 次 2h 修炼 ≈ 2000 修为,契合等级节奏设计)
 * - 到期自动结算并推送全额收益(无超时惩罚),默认时长 18~28 分钟随机;推送失败时暂存通知,下次发消息补送
 * - 提前收获:修炼满 10 分钟后可提前结束,收益 = 预期 × 进度 × (0.5 + 0.5×进度),越接近完成衰减越小
 */
public class CultivationService {
    private static final Logger log = LoggerFactory.getLogger(CultivationService.class);

    /** 修炼结算/到期提醒的推送回调,返回是否推送成功 */
    @FunctionalInterface
    public interface Notifier {
        boolean push(CultivationTask task, String message);
    }

    /** 社交效率倍率提供者(群活跃/互助/潮汐/惩罚等综合倍率) */
    @FunctionalInterface
    public interface SocialMultiplierProvider {
        double multiplier(String userId, String groupId);
    }

    /** 修为获得回调(用于宗门战统计) */
    @FunctionalInterface
    public interface ExpGainNotifier {
        void onExpGain(String userId, String groupId, long exp);
    }

    public static final int DEFAULT_MINUTES = 30;
    public static final int MAX_MINUTES = 120;
    public static final int MIN_HARVEST_MINUTES = 10;
    /** 每小时基础修为(另有时长加成:×(1 + 分钟数/120)) */
    public static final double BASE_EXP_PER_HOUR = 100;

    private final CultivationTaskRepository taskRepository;
    private final PlayerService playerService;
    private final NoticeRepository noticeRepository;
    @Setter
    private volatile Notifier notifier = (t, msg) -> {
        log.info("[推送] {}: {}", t.userId(), msg);
        return false;
    };
    @Setter
    private volatile SocialMultiplierProvider socialMultiplierProvider = (uid, gid) -> 1.0;
    @Setter
    private volatile ExpGainNotifier expGainNotifier = (uid, gid, exp) -> {};

    public CultivationService(CultivationTaskRepository taskRepository, PlayerService playerService,
                              NoticeRepository noticeRepository) {
        this.taskRepository = taskRepository;
        this.playerService = playerService;
        this.noticeRepository = noticeRepository;
    }

    public Optional<CultivationTask> activeTask(String userId) {
        return taskRepository.findActiveByUser(userId);
    }

    /** 默认修炼时长:18~28 分钟随机 */
    public static int randomDefaultMinutes() {
        return ThreadLocalRandom.current().nextInt(18, 29);
    }

    /**
     * 开始修炼(社交倍率默认 1.0,向后兼容)。
     *
     * @param method  功法,null 表示默认吐纳诀
     * @param minutes 修炼时长(分钟),≤0 表示默认 18~28 分钟随机
     */
    public String start(Player player, CultivationMethod method, int minutes) {
        return start(player, method, minutes, 1.0);
    }

    /**
     * 开始修炼。
     *
     * @param method          功法,null 表示默认吐纳诀
     * @param minutes         修炼时长(分钟),≤0 表示默认 18~28 分钟随机
     * @param socialMultiplier 社交效率倍率(群活跃/互助/潮汐/惩罚等综合)
     */
    public String start(Player player, CultivationMethod method, int minutes, double socialMultiplier) {
        Optional<CultivationTask> active = taskRepository.findActiveByUser(player.userId());
        if (active.isPresent()) {
            CultivationTask t = active.get();
            if (t.status() == CultivationTask.STATUS_READY || t.endTime() <= System.currentTimeMillis()) {
                return "道友上次修炼已完成,请先收获再开始新的修炼。";
            }
            long remain = Math.max(1, (t.endTime() - System.currentTimeMillis()) / 60_000 + 1);
            return "道友已在修炼中,剩余约 " + remain + " 分钟(满 " + MIN_HARVEST_MINUTES + " 分钟后可提前收获)。";
        }
        if (method == null) method = CultivationMethod.TU_NA;
        if (player.realm().ordinal() < method.requiredRealm().ordinal()) {
            return "《" + method.label() + "》需" + method.requiredRealm().label() + "及以上方可修炼。";
        }
        // 性别×方向联动功法检查
        if (method.genderReq() != null && player.gender() != method.genderReq()) {
            return "《" + method.label() + "》需" + method.genderReq().label() + "性修士方可修炼。";
        }
        if (method.directionReq() != null && player.direction() != method.directionReq()) {
            return "《" + method.label() + "》需" + method.directionReq().label() + "方向方可修炼。";
        }
        if (minutes <= 0) minutes = randomDefaultMinutes();
        if (minutes > MAX_MINUTES) return "单次修炼时长最长 2 小时(时长越长收益越高)。";
        double directionBonus = player.direction() == null ? 1.0 : player.direction().cultivationBonus();
        long reward = expectedReward(minutes, method, player.level(), socialMultiplier, directionBonus);
        taskRepository.insert(player.userId(), player.groupId(), method.label(), System.currentTimeMillis(), minutes, reward);
        return "道友开始修炼《" + method.label() + "》,预计 " + formatDuration(minutes) + " 后完成,将获得 +" + reward
                + " 修为,+" + stonesOf(reward) + " 灵石,完成后自动收获";
    }

    public static boolean isQuick(int minutes) {
        return minutes <= DEFAULT_MINUTES;
    }

    public long expectedReward(int minutes, CultivationMethod method) {
        return expectedReward(minutes, method, 1, 1.0);
    }

    public long expectedReward(int minutes, CultivationMethod method, int level) {
        return expectedReward(minutes, method, level, 1.0);
    }

    public long expectedReward(int minutes, CultivationMethod method, int level, double socialMultiplier) {
        return expectedReward(minutes, method, level, socialMultiplier, 1.0);
    }

    /** 预期修为 = 分钟数 × (1 + 分钟数/120) × 每分钟基础 × 功法倍率 × 境界压制 × 社交倍率 × 方向加成,时长越长效率越高。 */
    public long expectedReward(int minutes, CultivationMethod method, int level, double socialMultiplier, double directionBonus) {
        double multiplier = (method == null ? 1.0 : method.multiplier())
                * LevelTable.cultivationMultiplier(level) * socialMultiplier * directionBonus;
        return Math.round(minutes * (1 + minutes / 120.0) * (BASE_EXP_PER_HOUR / 60) * multiplier);
    }

    /** 计算社交效率倍率(由外部服务提供)。 */
    public double socialMultiplier(String userId, String groupId) {
        return socialMultiplierProvider.multiplier(userId, groupId);
    }

    /** 通知修为获得(用于宗门战统计)。 */
    private void notifyExpGain(String userId, String groupId, long exp) {
        if (exp > 0) expGainNotifier.onExpGain(userId, groupId, exp);
    }

    /** 灵石收益 = 修为收益 ÷ 2。 */
    public static long stonesOf(long exp) {
        return exp / 2;
    }

    /**
     * 到期任务处理,由调度器周期调用:到期即自动结算全额收益并推送(无超时惩罚)。
     */
    public void settleFinishedTasks() {
        List<CultivationTask> finished = taskRepository.findFinishedRunning(System.currentTimeMillis());
        for (CultivationTask task : finished) {
            try {
                settle(task);
            } catch (Exception e) {
                log.error("处理修炼任务 {} 失败", task.taskId(), e);
            }
        }
    }

    /** 到期自动结算全额收益:推送成功即完成,否则暂存通知待下次发消息补送。 */
    private void settle(CultivationTask task) {
        Optional<Player> opt = playerService.find(task.userId());
        if (opt.isEmpty()) {
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_EXPIRED);
            return;
        }
        Player player = opt.get();
        long stones = stonesOf(task.expectedReward());
        player.addSpiritStones(stones);
        String levelTip = playerService.gainExp(player, task.expectedReward());
        notifyExpGain(task.userId(), task.groupId(), task.expectedReward());
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        String message = "道友修炼完成!获得 +" + task.expectedReward() + " 修为,+" + stones
                + " 灵石," + progressOf(player) + levelTip;
        if (!notifier.push(task, message)) {
            noticeRepository.add(task.userId(), message);
        }
    }

    /**
     * 收获修炼(!收获):到期已自动结算,一般无需手动收获;即使手动收获也为全额收益(无超时惩罚);
     * 也可提前结束——满 10 分钟后收益 = 预期 × 进度 × (0.5 + 0.5×进度),不足 10 分钟无任何收益。
     */
    public String harvest(Player player) {
        Optional<CultivationTask> opt = taskRepository.findActiveByUser(player.userId());
        if (opt.isEmpty()) return "道友当前没有可收获的修炼。";
        CultivationTask task = opt.get();
        long now = System.currentTimeMillis();
        if (now < task.endTime()) {
            long elapsedMinutes = (now - task.startTime()) / 60_000;
            if (elapsedMinutes < MIN_HARVEST_MINUTES) {
                return "修炼不足 " + MIN_HARVEST_MINUTES + " 分钟,提前收获不会获得任何收益,请继续修炼。";
            }
            double progress = (double) (now - task.startTime()) / (task.endTime() - task.startTime());
            long gain = Math.round(task.expectedReward() * progress * (0.5 + 0.5 * progress));
            player.addSpiritStones(stonesOf(gain));
            String levelTip = playerService.gainExp(player, gain);
            notifyExpGain(player.userId(), player.groupId(), gain);
            taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
            return "道友提前结束修炼(进度 " + Math.round(progress * 100) + "%),获得 +" + gain
                    + " 修为,+" + stonesOf(gain) + " 灵石(提前收获有衰减)\n当前" + progressOf(player) + levelTip;
        }
        // 已到期(含超时):全额结算,无超时惩罚,与自动收获一致
        settle(task);
        return "道友修炼完成!获得 +" + task.expectedReward() + " 修为,+" + stonesOf(task.expectedReward())
                + " 灵石,当前"
                + playerService.find(player.userId()).map(CultivationService::progressOf).orElse("");
    }

    /** 当前等级与修为进度描述。 */
    public static String progressOf(Player player) {
        if (player.level() >= LevelTable.MAX_LEVEL) {
            return "Lv." + player.level() + ",修为:" + player.exp() + "(已满级)";
        }
        String suffix = LevelTable.atBreakthrough(player.level()) ? "(需突破)" : "";
        return "Lv." + player.level() + ",修为:" + player.exp() + "/"
                + LevelTable.expToNextLevel(player.level()) + suffix;
    }

    /** 加速丹:立即完成当前修炼，获得全额收益（无溢出惩罚）。 */
    public String speedFinish(Player player) {
        Optional<CultivationTask> opt = taskRepository.findActiveByUser(player.userId());
        if (opt.isEmpty()) return "道友当前没有进行中的修炼。";
        CultivationTask task = opt.get();
        long baseExp = task.expectedReward();
        long stones = stonesOf(baseExp);
        player.addSpiritStones(stones);
        String levelTip = playerService.gainExp(player, baseExp);
        notifyExpGain(player.userId(), player.groupId(), baseExp);
        taskRepository.updateStatus(task.taskId(), CultivationTask.STATUS_DONE);
        return "道友服下加速丹,修炼瞬间完成!获得 +" + baseExp + " 修为,+" + stones
                + " 灵石\n当前" + progressOf(player) + levelTip;
    }

    public static String formatDuration(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) return (minutes / 60) + " 小时";
        if (minutes > 60) return (minutes / 60) + " 小时 " + (minutes % 60) + " 分钟";
        return minutes + " 分钟";
    }
}

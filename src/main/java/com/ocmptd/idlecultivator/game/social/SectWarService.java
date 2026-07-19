package com.ocmptd.idlecultivator.game.social;

import com.ocmptd.idlecultivator.storage.SectWarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 宗门战服务:以 QQ 群为单位每周比拼总修为。
 * <p>
 * - 每次修炼结算时累加群修为到当周记录
 * - !宗门排行 查看本周排行
 * - 每周一 0:00 自动结算上周排行
 */
public class SectWarService {
    private static final Logger log = LoggerFactory.getLogger(SectWarService.class);

    private final SectWarRepository repository;

    public SectWarService(SectWarRepository repository) {
        this.repository = repository;
    }

    /** 累加修炼修为到群的当周记录(由修炼结算回调)。 */
    public void addExp(String groupId, long exp) {
        if (groupId == null || exp <= 0) return;
        String weekKey = GroupService.currentWeekKey();
        repository.addExp(weekKey, groupId, exp);
    }

    /** 获取本周排行(前 10 名)。 */
    public List<SectWarRepository.Entry> currentRanking() {
        return repository.ranking(GroupService.currentWeekKey(), 10);
    }

    /** 获取某群本周排名。 */
    public int currentRankOf(String groupId) {
        return repository.rankOf(GroupService.currentWeekKey(), groupId);
    }

    /** 排行榜描述文案。 */
    public String describeRanking(String groupId) {
        List<SectWarRepository.Entry> ranking = currentRanking();
        if (ranking.isEmpty()) {
            return "本周宗门战暂无数据,开始修炼即可为本群积累修为!";
        }
        StringBuilder sb = new StringBuilder("=== 宗门战周排行(")
                .append(GroupService.currentWeekKey()).append(") ===");
        for (int i = 0; i < ranking.size(); i++) {
            SectWarRepository.Entry e = ranking.get(i);
            String marker = e.groupId().equals(groupId) ? " ← 本群" : "";
            sb.append("\n").append(i + 1).append(". 群").append(maskGroupId(e.groupId()))
                    .append(":").append(e.totalExp()).append(" 修为").append(marker);
        }
        if (groupId != null) {
            int rank = currentRankOf(groupId);
            if (rank > 0) {
                sb.append("\n本群排名第 ").append(rank);
            } else {
                sb.append("\n本群暂未上榜,加油修炼!");
            }
        }
        sb.append("\n每周一 0:00 结算,修为最高的群将获得灵气加成!");
        return sb.toString();
    }

    /** 每周结算(由调度器在周一 0:00 调用)。 */
    public String weeklySettlement() {
        String lastWeek = GroupService.lastWeekKey();
        List<SectWarRepository.Entry> ranking = repository.lastWeekRanking(lastWeek);
        if (ranking.isEmpty()) {
            log.info("宗门战周结算:上周({})无数据", lastWeek);
            return null;
        }
        SectWarRepository.Entry winner = ranking.get(0);
        String msg = "=== 宗门战周结算(" + lastWeek + ") ===\n" +
                "冠军:群" + maskGroupId(winner.groupId()) + " 总修为:" + winner.totalExp() + "\n" +
                "恭喜夺冠!本周该群将获得额外修炼加成!";
        log.info("宗门战周结算完成:上周冠军 群{} 修为{}", winner.groupId(), winner.totalExp());
        return msg;
    }

    /** 脱敏群 ID(只显示后 4 位)。 */
    private static String maskGroupId(String groupId) {
        if (groupId == null) return "???";
        if (groupId.length() <= 4) return "****";
        return "****" + groupId.substring(groupId.length() - 4);
    }
}

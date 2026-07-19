package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.SectWarService;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.SectWarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectWarServiceTest {
    private Database db;
    private SectWarService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        service = new SectWarService(new SectWarRepository(db));
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void addExpAccumulates() {
        service.addExp("g1", 100);
        service.addExp("g1", 200);
        service.addExp("g2", 150);
        assertEquals(1, service.currentRankOf("g1"));
        assertEquals(2, service.currentRankOf("g2"));
    }

    @Test
    void rankingSortedByExpDesc() {
        service.addExp("g3", 50);
        service.addExp("g1", 300);
        service.addExp("g2", 200);
        List<SectWarRepository.Entry> ranking = service.currentRanking();
        assertEquals(3, ranking.size());
        assertEquals("g1", ranking.get(0).groupId());
        assertEquals(300, ranking.get(0).totalExp());
        assertEquals("g2", ranking.get(1).groupId());
        assertEquals("g3", ranking.get(2).groupId());
    }

    @Test
    void describeRankingContainsInfo() {
        service.addExp("g1", 100);
        String desc = service.describeRanking("g1");
        assertTrue(desc.contains("宗门战"), desc);
        assertTrue(desc.contains("排名第 1"), desc);
    }

    @Test
    void describeRankingEmptyWhenNoData() {
        String desc = service.describeRanking("g1");
        assertTrue(desc.contains("暂无数据"), desc);
    }

    @Test
    void rankOfReturnsZeroForUnknownGroup() {
        service.addExp("g1", 100);
        assertEquals(0, service.currentRankOf("g2"));
    }

    @Test
    void weeklySettlementReturnsWinner() {
        // 往上周添加数据(weeklySettlement 查询的是上周排行)
        String lastWeek = GroupService.lastWeekKey();
        SectWarRepository repo = new SectWarRepository(db);
        repo.addExp(lastWeek, "g1", 500);
        repo.addExp(lastWeek, "g2", 300);
        String result = service.weeklySettlement();
        assertTrue(result != null);
        assertTrue(result.contains("冠军"), result);
        assertTrue(result.contains("500"), result);
    }

    @Test
    void weeklySettlementReturnsNullWhenNoData() {
        String result = service.weeklySettlement();
        assertEquals(null, result);
    }
}

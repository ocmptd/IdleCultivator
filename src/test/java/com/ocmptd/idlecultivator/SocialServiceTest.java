package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.social.SocialService;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import com.ocmptd.idlecultivator.storage.SocialRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialServiceTest {
    private Database db;
    private PlayerService playerService;
    private SocialService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        playerService = new PlayerService(new PlayerRepository(db));
        service = new SocialService(new SocialRepository(db), playerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void mutualHelpGivesBonus() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        playerService.create("u2", "g1", "李四", Gender.FEMALE);
        String result = service.requestHelp("u1", "u2", "g1");
        assertTrue(result.contains("互助"), result);
        // 互助后双方都有1层加成
        assertEquals(1.2, service.helpMultiplier("u1"), 0.01);
        assertEquals(1.2, service.helpMultiplier("u2"), 0.01);
    }

    @Test
    void mutualHelpMaxStacks() {
        playerService.create("u1", "g1", "甲", Gender.MALE);
        playerService.create("u2", "g1", "乙", Gender.MALE);
        playerService.create("u3", "g1", "丙", Gender.MALE);
        playerService.create("u4", "g1", "丁", Gender.MALE);
        service.requestHelp("u1", "u2", "g1");
        service.requestHelp("u1", "u3", "g1");
        service.requestHelp("u1", "u4", "g1");
        // 已满3层
        assertEquals(1.6, service.helpMultiplier("u1"), 0.01);
        // 第4次应被拒绝
        playerService.create("u5", "g1", "戊", Gender.MALE);
        String result = service.requestHelp("u1", "u5", "g1");
        assertTrue(result.contains("已满"), result);
    }

    @Test
    void mutualHelpRejectsSelf() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        String result = service.requestHelp("u1", "u1", "g1");
        assertTrue(result.contains("不可与自己"), result);
    }

    @Test
    void mutualHelpRejectsUnknownTarget() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        String result = service.requestHelp("u1", "unknown", "g1");
        assertTrue(result.contains("尚未创建角色"), result);
    }

    @Test
    void daoCompanionGivesBonus() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        playerService.create("u2", "g1", "李四", Gender.FEMALE);
        String result = service.establishDaoCompanion("u1", "u2", "g1");
        assertTrue(result.contains("道侣"), result);
        assertEquals(1.1, service.daoCompanionMultiplier("u1"), 0.01);
        assertEquals(1.1, service.daoCompanionMultiplier("u2"), 0.01);
        // 双向查询
        Optional<String> partner = service.findPartnerId("u1");
        assertTrue(partner.isPresent());
        assertEquals("u2", partner.get());
        partner = service.findPartnerId("u2");
        assertTrue(partner.isPresent());
        assertEquals("u1", partner.get());
    }

    @Test
    void daoCompanionRejectsExisting() {
        playerService.create("u1", "g1", "甲", Gender.MALE);
        playerService.create("u2", "g1", "乙", Gender.FEMALE);
        playerService.create("u3", "g1", "丙", Gender.FEMALE);
        service.establishDaoCompanion("u1", "u2", "g1");
        String result = service.establishDaoCompanion("u1", "u3", "g1");
        assertTrue(result.contains("已有道侣"), result);
    }

    @Test
    void breakDaoCompanionRemovesBoth() {
        playerService.create("u1", "g1", "甲", Gender.MALE);
        playerService.create("u2", "g1", "乙", Gender.FEMALE);
        service.establishDaoCompanion("u1", "u2", "g1");
        String result = service.breakDaoCompanion("u1");
        assertTrue(result.contains("解除"), result);
        assertTrue(service.findPartnerId("u1").isEmpty());
        assertTrue(service.findPartnerId("u2").isEmpty());
    }

    @Test
    void inactivityMultiplierNoPenaltyForActive() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        service.updateActivity("u1");
        assertEquals(1.0, service.inactivityMultiplier("u1"), 0.01);
    }

    @Test
    void inactivityMultiplierPenalizesInactive() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        // 设置最后活跃时间为25小时前
        var p = playerService.find("u1").orElseThrow();
        p.setLastActiveTime(System.currentTimeMillis() - 25 * 60 * 60_000L);
        playerService.save(p);
        assertEquals(0.9, service.inactivityMultiplier("u1"), 0.01);

        // 50小时前
        p.setLastActiveTime(System.currentTimeMillis() - 50 * 60 * 60_000L);
        playerService.save(p);
        assertEquals(0.8, service.inactivityMultiplier("u1"), 0.01);

        // 80小时前
        p.setLastActiveTime(System.currentTimeMillis() - 80 * 60 * 60_000L);
        playerService.save(p);
        assertEquals(0.5, service.inactivityMultiplier("u1"), 0.01);
    }

    @Test
    void socialMultiplierCombinesAll() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        playerService.create("u2", "g1", "李四", Gender.FEMALE);
        service.updateActivity("u1");
        service.requestHelp("u1", "u2", "g1");
        service.establishDaoCompanion("u1", "u2", "g1");
        // 互助1.2 × 道侣1.1 × 无惩罚1.0 = 1.32
        assertEquals(1.32, service.socialMultiplier("u1"), 0.01);
    }
}

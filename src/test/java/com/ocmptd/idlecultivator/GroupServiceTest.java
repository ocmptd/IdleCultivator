package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.social.GroupService;
import com.ocmptd.idlecultivator.game.social.GroupStatus;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.GroupStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupServiceTest {
    private Database db;
    private GroupService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        service = new GroupService(new GroupStatusRepository(db));
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void getOrCreateReturnsDefaultStatus() {
        GroupStatus gs = service.getGroupStatus("g1");
        assertNotNull(gs);
        assertEquals(1, gs.activityLevel()); // 默认平和
        assertEquals(1.0, gs.activityMultiplier());
    }

    @Test
    void recordMessageIncrementsCount() {
        service.recordMessage("g1");
        service.recordMessage("g1");
        service.recordMessage("g1");
        GroupStatus gs = service.getGroupStatus("g1");
        assertEquals(3, gs.messageCount());
        assertEquals(3, gs.hourlyMsgCount());
    }

    @Test
    void groupMultiplierReflectsActivityLevel() {
        GroupStatus gs = service.getGroupStatus("g1");
        gs.setActivityLevel(3); // 鼎盛
        service.getGroupStatus("g1"); // 确保存在
        // 重新保存
        gs.setActivityLevel(3);
        new GroupStatusRepository(db).save(gs);
        double mult = service.groupMultiplier("g1");
        assertEquals(1.5, mult, 0.01);
    }

    @Test
    void computeActivityLevelThresholds() {
        assertEquals(0, GroupService.computeActivityLevel(0));   // 稀薄
        assertEquals(1, GroupService.computeActivityLevel(1));   // 平和
        assertEquals(1, GroupService.computeActivityLevel(10));  // 平和
        assertEquals(2, GroupService.computeActivityLevel(11));  // 充沛
        assertEquals(2, GroupService.computeActivityLevel(50));  // 充沛
        assertEquals(3, GroupService.computeActivityLevel(51));  // 鼎盛
    }

    @Test
    void updateActivityLevelsResetsHourlyCount() {
        // 记录大量消息
        for (int i = 0; i < 60; i++) service.recordMessage("g1");
        GroupStatus gs = service.getGroupStatus("g1");
        assertEquals(60, gs.hourlyMsgCount());
        assertEquals(1, gs.activityLevel()); // 还是默认平和

        // 更新活跃度
        service.updateActivityLevels();
        gs = service.getGroupStatus("g1");
        assertEquals(0, gs.hourlyMsgCount()); // 已重置
        assertEquals(3, gs.activityLevel()); // 60条 → 鼎盛
    }

    @Test
    void weekKeyFormatValid() {
        String weekKey = GroupService.currentWeekKey();
        assertTrue(weekKey.matches("\\d{4}-W\\d{2}"), "Week key should match pattern: " + weekKey);
    }

    @Test
    void describeContainsActivityInfo() {
        service.recordMessage("g1");
        GroupStatus gs = service.getGroupStatus("g1");
        String desc = service.describe(gs);
        assertTrue(desc.contains("活跃度"));
        assertTrue(desc.contains("总消息数:1"));
    }
}

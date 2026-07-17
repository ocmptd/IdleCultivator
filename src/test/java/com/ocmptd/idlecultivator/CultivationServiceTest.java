package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.cultivation.CultivationMethod;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.cultivation.CultivationTask;
import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationServiceTest {
    private Database db;
    private PlayerService playerService;
    private CultivationTaskRepository taskRepository;
    private CultivationService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        playerService = new PlayerService(new PlayerRepository(db));
        taskRepository = new CultivationTaskRepository(db);
        service = new CultivationService(taskRepository, playerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private Player newPlayer() {
        return playerService.create("u1", "g1", "张三", Gender.MALE);
    }

    /** 将任务开始时间回拨,模拟时间流逝。 */
    private void backdateTask(long taskId, long millis) throws Exception {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "UPDATE cultivation_tasks SET start_time = start_time - ? WHERE task_id = ?")) {
            ps.setLong(1, millis);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        }
    }

    private long activeTaskId() {
        return service.activeTask("u1").orElseThrow().taskId();
    }

    @Test
    void quickCultivationAutoSettlesWithPush() throws Exception {
        Player p = newPlayer();
        String start = service.start(p, null, 30);
        assertTrue(start.contains("+63 修为"), start); // 30 × 1.25 × 100/60 ≈ 63
        backdateTask(activeTaskId(), 31 * 60_000L);

        AtomicReference<String> pushed = new AtomicReference<>();
        service.setNotifier((task, msg) -> {
            pushed.set(msg);
            return true;
        });
        service.settleFinishedTasks();

        assertNotNull(pushed.get());
        assertTrue(pushed.get().contains("+63 修为"), pushed.get());
        Player reloaded = playerService.find("u1").orElseThrow();
        assertEquals(63, reloaded.exp());
        assertEquals(31, reloaded.spiritStones());
    }

    @Test
    void longCultivationNeedsHarvest() throws Exception {
        Player p = newPlayer();
        String start = service.start(p, CultivationMethod.TU_NA, 120);
        assertTrue(start.contains("+400 修为"), start); // 120 × 2 × 100/60 = 400
        backdateTask(activeTaskId(), 120 * 60_000L + 30_000L); // 超时不足 1 分钟,无溢出

        AtomicReference<String> pushed = new AtomicReference<>();
        service.setNotifier((task, msg) -> {
            pushed.set(msg);
            return true;
        });
        service.settleFinishedTasks();
        assertTrue(pushed.get().contains("可以收获"), pushed.get());
        assertEquals(CultivationTask.STATUS_READY, service.activeTask("u1").orElseThrow().status());

        String result = service.harvest(playerService.find("u1").orElseThrow());
        assertTrue(result.contains("+400 修为"), result);
        // 400 修为自动升级:110+120+130=360,剩余 40
        assertEquals(4, playerService.find("u1").orElseThrow().level());
        assertEquals(40, playerService.find("u1").orElseThrow().exp());
        assertEquals(200, playerService.find("u1").orElseThrow().spiritStones());
        assertTrue(service.activeTask("u1").isEmpty());
    }

    @Test
    void lightOverflowConvertsToDust() throws Exception {
        Player p = newPlayer();
        service.start(p, null, 120); // 2h → +400 修为
        backdateTask(activeTaskId(), (120 + 30) * 60_000L); // 超时 30 分钟

        String result = service.harvest(playerService.find("u1").orElseThrow());
        assertTrue(result.contains("灵尘"), result);
        Player reloaded = playerService.find("u1").orElseThrow();
        assertTrue(result.contains("+350 修为"), result); // 溢出 30/60×100=50 折损
        assertTrue(Inventory.parse(reloaded.inventory()).containsKey("灵尘"));
    }

    @Test
    void earlyHarvestUnderTenMinutesRejected() {
        Player p = newPlayer();
        service.start(p, null, 120);
        String result = service.harvest(p);
        assertTrue(result.contains("不足 10 分钟"), result);
        assertTrue(service.activeTask("u1").isPresent());
    }

    @Test
    void earlyHarvestDecaysReward() throws Exception {
        Player p = newPlayer();
        service.start(p, null, 120); // 2h → +400 修为
        backdateTask(activeTaskId(), 60 * 60_000L); // 修炼进度 50%

        String result = service.harvest(playerService.find("u1").orElseThrow());
        assertTrue(result.contains("提前结束"), result);
        // 400 × 0.5 × (0.5 + 0.25) = 150,升 Lv.2 耗 110 剩 40
        assertTrue(result.contains("+150 修为"), result);
        assertEquals(2, playerService.find("u1").orElseThrow().level());
        assertTrue(service.activeTask("u1").isEmpty());
    }

    @Test
    void pushFailureAutoSettlesLongTask() throws Exception {
        Player p = newPlayer();
        service.start(p, null, 120); // 默认 notifier 返回 false(推送不可用)
        backdateTask(activeTaskId(), 121 * 60_000L);

        service.settleFinishedTasks();
        assertEquals(4, playerService.find("u1").orElseThrow().level());
        assertEquals(40, playerService.find("u1").orElseThrow().exp());
        assertTrue(service.activeTask("u1").isEmpty());
    }

    @Test
    void repeatCultivateShowsRemaining() {
        Player p = newPlayer();
        service.start(p, null, 120);
        String result = service.start(p, null, 30);
        assertTrue(result.contains("剩余约"), result);
    }

    @Test
    void maxTwoHoursEnforced() {
        Player p = newPlayer();
        String result = service.start(p, null, 121);
        assertTrue(result.contains("最长 2 小时"), result);
        assertTrue(service.activeTask("u1").isEmpty());
    }

    @Test
    void longerCultivationYieldsMorePerHour() {
        assertTrue(service.expectedReward(120, null) > 2 * service.expectedReward(60, null));
        assertTrue(service.expectedReward(60, null) > 2 * service.expectedReward(30, null));
    }

    @Test
    void methodRequiresRealm() {
        Player p = newPlayer();
        String result = service.start(p, CultivationMethod.XUAN_TIAN, 60);
        assertTrue(result.contains("需金丹期"), result);
    }
}

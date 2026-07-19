package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.social.Beast;
import com.ocmptd.idlecultivator.game.social.BeastService;
import com.ocmptd.idlecultivator.storage.BeastRepository;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeastServiceTest {
    private Database db;
    private PlayerService playerService;
    private BeastService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        playerService = new PlayerService(new PlayerRepository(db));
        service = new BeastService(new BeastRepository(db), playerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    void attackWithoutBeastReturnsMessage() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        String result = service.attack("u1", "g1");
        assertTrue(result.contains("暂无异兽"), result);
    }

    @Test
    void attackWithoutPlayerReturnsMessage() {
        String result = service.attack("u1", "g1");
        assertTrue(result.contains("尚未创建角色"), result);
    }

    @Test
    void attackWithoutGroupReturnsMessage() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        String result = service.attack("u1", null);
        assertTrue(result.contains("群内"), result);
    }

    @Test
    void spawnAndKillBeast() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        // 手动创建一个低 HP 的异兽便于测试
        Beast beast = new Beast("g1", "测试妖兽", 10, 10, 50, 25, System.currentTimeMillis());
        new BeastRepository(db).save(beast);

        // 玩家等级1,伤害 = 1*10 + random(1~20) = 11~30,必杀
        String result = service.attack("u1", "g1");
        assertTrue(result.contains("击杀"), result);
        assertTrue(result.contains("+50 修为"), result);
        // 异兽已被删除
        assertTrue(service.currentBeast("g1").isEmpty());
        // 验证玩家获得了修为和灵石
        var p = playerService.find("u1").orElseThrow();
        assertTrue(p.exp() > 0);
        assertTrue(p.spiritStones() > 0);
    }

    @Test
    void attackDamagesBeastWithoutKill() {
        playerService.create("u1", "g1", "张三", Gender.MALE);
        // HP 很高的异兽,一次攻击无法击杀
        Beast beast = new Beast("g1", "高阶妖兽", 1000, 1000, 400, 200, System.currentTimeMillis());
        new BeastRepository(db).save(beast);

        String result = service.attack("u1", "g1");
        assertTrue(result.contains("伤害"), result);
        assertTrue(result.contains("剩余 HP"), result);
        // 异兽仍在
        Optional<Beast> current = service.currentBeast("g1");
        assertTrue(current.isPresent());
        assertTrue(current.get().hp() < 1000); // HP 已减少
    }

    @Test
    void currentBeastReturnsEmptyWhenNone() {
        assertTrue(service.currentBeast("g1").isEmpty());
    }

    @Test
    void currentBeastReturnsEmptyAfterExpiry() throws Exception {
        // 创建一个已过期的异兽(spawnTime 在 2 小时前)
        Beast beast = new Beast("g1", "过期妖兽", 100, 100, 50, 25,
                System.currentTimeMillis() - 2 * 60 * 60_000L);
        new BeastRepository(db).save(beast);
        // 查询时自动清理
        assertTrue(service.currentBeast("g1").isEmpty());
    }
}

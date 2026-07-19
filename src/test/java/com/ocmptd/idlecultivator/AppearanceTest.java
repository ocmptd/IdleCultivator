package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.command.commands.AppearanceCommand;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.player.Realm;
import com.ocmptd.idlecultivator.game.portrait.PortraitService;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppearanceTest {
    private Database db;
    private PlayerService playerService;
    private PortraitService portraitService;
    private AppearanceCommand hairstyleCmd;
    private AppearanceCommand outfitCmd;
    private AppearanceCommand accessoryCmd;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        playerService = new PlayerService(new PlayerRepository(db));
        portraitService = new PortraitService();
        hairstyleCmd = new AppearanceCommand("换发型", "发型", playerService);
        outfitCmd = new AppearanceCommand("换服饰", "服饰", playerService);
        accessoryCmd = new AppearanceCommand("换配饰", "配饰", playerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private Player newPlayer() {
        return playerService.create("u1", "g1", "测试", Gender.FEMALE);
    }

    private Player zhuJiPlayer() {
        Player p = newPlayer();
        p.setLevel(21);
        p.setRealm(Realm.ZHU_JI);
        playerService.save(p);
        return p;
    }

    private Player jinDanPlayer() {
        Player p = newPlayer();
        p.setLevel(41);
        p.setRealm(Realm.JIN_DAN);
        playerService.save(p);
        return p;
    }

    private CommandContext ctx(String... args) {
        return new CommandContext("u1", "g1", List.of(args), "!");
    }

    // === 发型 ===

    @Test
    void hairstyleFirstTimeFree() {
        newPlayer();
        String result = hairstyleCmd.execute(ctx("素髻"));
        assertTrue(result.contains("素髻"), result);
        assertFalse(result.contains("灵石")); // 首次免费不提灵石消耗
        Player p = playerService.find("u1").orElseThrow();
        assertEquals("素髻", p.hairstyle());
    }

    @Test
    void hairstyleChangeCostsSpiritStones() {
        Player p = newPlayer();
        p.setSpiritStones(100);
        playerService.save(p);
        hairstyleCmd.execute(ctx("素髻")); // 首次免费
        String result = hairstyleCmd.execute(ctx("素髻")); // 换回同样拒绝
        assertTrue(result.contains("无需更换"), result);
    }

    @Test
    void hairstyleRequiresRealm() {
        newPlayer(); // 练气期
        String result = hairstyleCmd.execute(ctx("道髻"));
        assertTrue(result.contains("筑基期"), result);
    }

    @Test
    void hairstyleUnlockedAtZhuJi() {
        zhuJiPlayer();
        String result = hairstyleCmd.execute(ctx("道髻"));
        assertTrue(result.contains("道髻"), result);
        Player p = playerService.find("u1").orElseThrow();
        assertEquals("道髻", p.hairstyle());
    }

    @Test
    void hairstyleListShowsOptions() {
        newPlayer();
        String result = hairstyleCmd.execute(ctx());
        assertTrue(result.contains("素髻"), result);
        assertTrue(result.contains("道髻"), result);
        assertTrue(result.contains("飞仙髻"), result);
    }

    @Test
    void hairstyleChangeCostsStones() {
        Player p = zhuJiPlayer();
        p.setSpiritStones(200);
        playerService.save(p);
        hairstyleCmd.execute(ctx("素髻")); // 首次免费
        String result = hairstyleCmd.execute(ctx("道髻")); // 更换需 50 灵石
        assertTrue(result.contains("50"), result);
        Player reloaded = playerService.find("u1").orElseThrow();
        assertEquals("道髻", reloaded.hairstyle());
        assertEquals(150, reloaded.spiritStones());
    }

    // === 服饰 ===

    @Test
    void outfitFirstTimeFree() {
        newPlayer();
        String result = outfitCmd.execute(ctx("麻衣"));
        assertTrue(result.contains("麻衣"), result);
        Player p = playerService.find("u1").orElseThrow();
        assertEquals("麻衣", p.outfit());
    }

    @Test
    void outfitRequiresRealm() {
        newPlayer();
        String result = outfitCmd.execute(ctx("道袍"));
        assertTrue(result.contains("筑基期"), result);
    }

    @Test
    void outfitChangeCostsStones() {
        Player p = zhuJiPlayer();
        p.setSpiritStones(300);
        playerService.save(p);
        outfitCmd.execute(ctx("麻衣")); // 首次免费
        String result = outfitCmd.execute(ctx("道袍")); // 更换需 100 灵石
        assertTrue(result.contains("100"), result);
        assertEquals("道袍", playerService.find("u1").orElseThrow().outfit());
        assertEquals(200, playerService.find("u1").orElseThrow().spiritStones());
    }

    // === 配饰 ===

    @Test
    void accessoryRequiresZhuJi() {
        newPlayer(); // 练气期
        String result = accessoryCmd.execute(ctx("玉佩"));
        assertTrue(result.contains("筑基期"), result);
    }

    @Test
    void accessoryFirstTimeFreeAtZhuJi() {
        zhuJiPlayer();
        String result = accessoryCmd.execute(ctx("玉佩"));
        assertTrue(result.contains("玉佩"), result);
        Player p = playerService.find("u1").orElseThrow();
        assertEquals("玉佩", p.accessory());
    }

    @Test
    void accessoryChangeCostsStones() {
        Player p = jinDanPlayer();
        p.setSpiritStones(500);
        playerService.save(p);
        accessoryCmd.execute(ctx("玉佩")); // 首次免费
        String result = accessoryCmd.execute(ctx("灵环")); // 更换需 150 灵石
        assertTrue(result.contains("150"), result);
        assertEquals("灵环", playerService.find("u1").orElseThrow().accessory());
    }

    // === 还原默认 ===

    @Test
    void hairstyleResetToDefault() {
        Player p = zhuJiPlayer();
        hairstyleCmd.execute(ctx("道髻"));
        assertEquals("道髻", playerService.find("u1").orElseThrow().hairstyle());
        String result = hairstyleCmd.execute(ctx("默认"));
        assertTrue(result.contains("还原"), result);
        assertNull(playerService.find("u1").orElseThrow().hairstyle());
    }

    @Test
    void accessoryResetToDefaultFree() {
        Player p = jinDanPlayer();
        p.setSpiritStones(500);
        playerService.save(p);
        accessoryCmd.execute(ctx("玉佩")); // 首次免费
        String result = accessoryCmd.execute(ctx("卸下"));
        assertTrue(result.contains("还原"), result);
        Player reloaded = playerService.find("u1").orElseThrow();
        assertNull(reloaded.accessory());
        assertEquals(500, reloaded.spiritStones()); // 还原不扣灵石
    }

    @Test
    void resetWhenAlreadyDefault() {
        newPlayer();
        String result = hairstyleCmd.execute(ctx("默认"));
        assertTrue(result.contains("无需还原"), result);
    }

    // === 外观展示 ===

    @Test
    void customAppearanceInPortrait() {
        Player p = zhuJiPlayer();
        p.setHairstyle("飞仙髻");
        p.setOutfit("法袍");
        p.setAccessory("玉佩");
        String desc = portraitService.appearancePrompt(p);
        assertTrue(desc.contains("飞仙髻"), desc);
        assertTrue(desc.contains("法袍"), desc);
        assertTrue(desc.contains("玉佩"), desc);
    }

    @Test
    void defaultAppearanceWhenNotSet() {
        Player p = newPlayer();
        String desc = portraitService.appearancePrompt(p);
        assertTrue(desc.contains("青丝如瀑"), desc); // 默认发型
        assertTrue(desc.contains("麻衣"), desc); // 默认服饰
        assertFalse(desc.contains("佩戴"), desc); // 无配饰
    }

    @Test
    void appearanceShownInStatus() {
        Player p = zhuJiPlayer();
        p.setHairstyle("道髻");
        playerService.save(p);
        String desc = playerService.describe(playerService.find("u1").orElseThrow());
        assertTrue(desc.contains("发型"), desc);
        assertTrue(desc.contains("道髻"), desc);
    }

    // === 持久化 ===

    @Test
    void appearancePersistedAcrossReload() {
        Player p = zhuJiPlayer();
        p.setHairstyle("道髻");
        p.setOutfit("道袍");
        p.setAccessory("玉佩");
        playerService.save(p);

        Player reloaded = playerService.find("u1").orElseThrow();
        assertEquals("道髻", reloaded.hairstyle());
        assertEquals("道袍", reloaded.outfit());
        assertEquals("玉佩", reloaded.accessory());
    }

    @Test
    void overflowProtectedPersisted() {
        Player p = zhuJiPlayer();
        p.setOverflowProtected(true);
        playerService.save(p);

        Player reloaded = playerService.find("u1").orElseThrow();
        assertTrue(reloaded.overflowProtected());
    }
}

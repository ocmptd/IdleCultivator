package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.command.CommandContext;
import com.ocmptd.idlecultivator.command.commands.ChooseDirectionCommand;
import com.ocmptd.idlecultivator.game.cultivation.CultivationMethod;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.player.*;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.NoticeRepository;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {
    private Database db;
    private PlayerService playerService;
    private CultivationService cultivationService;
    private ChooseDirectionCommand directionCommand;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(":memory:");
        playerService = new PlayerService(new PlayerRepository(db));
        cultivationService = new CultivationService(
                new CultivationTaskRepository(db), playerService, new NoticeRepository(db));
        directionCommand = new ChooseDirectionCommand(playerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private Player newPlayer(Gender gender) {
        return playerService.create("u1", "g1", "测试", gender);
    }

    private Player zhuJiPlayer(Gender gender) {
        Player p = newPlayer(gender);
        p.setLevel(21);
        p.setRealm(Realm.ZHU_JI);
        playerService.save(p);
        return p;
    }

    private Player jinDanPlayer(Gender gender) {
        Player p = newPlayer(gender);
        p.setLevel(41);
        p.setRealm(Realm.JIN_DAN);
        playerService.save(p);
        return p;
    }

    private CommandContext ctx(String... args) {
        return new CommandContext("u1", "g1", List.of(args), "!");
    }

    // === 修炼方向选择 ===

    @Test
    void chooseDirectionRequiresZhuJi() {
        newPlayer(Gender.MALE);
        String result = directionCommand.execute(ctx("剑修"));
        assertTrue(result.contains("筑基期"), result);
    }

    @Test
    void chooseDirectionSuccess() {
        zhuJiPlayer(Gender.MALE);
        String result = directionCommand.execute(ctx("剑修"));
        assertTrue(result.contains("剑修"), result);
        Player p = playerService.find("u1").orElseThrow();
        assertEquals(CultivationDirection.SWORD, p.direction());
    }

    @Test
    void chooseDirectionChangeCostsSpiritStones() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setSpiritStones(1000);
        playerService.save(p);
        directionCommand.execute(ctx("剑修"));
        String result = directionCommand.execute(ctx("法修"));
        assertTrue(result.contains("500"), result);
        Player reloaded = playerService.find("u1").orElseThrow();
        assertEquals(CultivationDirection.MAGIC, reloaded.direction());
        assertEquals(500, reloaded.spiritStones());
    }

    @Test
    void chooseDirectionChangeInsufficientStones() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setSpiritStones(100);
        playerService.save(p);
        directionCommand.execute(ctx("剑修"));
        String result = directionCommand.execute(ctx("法修"));
        assertTrue(result.contains("灵石"), result);
        assertTrue(result.contains("100"), result);
    }

    @Test
    void chooseDirectionSameRejected() {
        zhuJiPlayer(Gender.MALE);
        directionCommand.execute(ctx("剑修"));
        String result = directionCommand.execute(ctx("剑修"));
        assertTrue(result.contains("无需重复"), result);
    }

    @Test
    void chooseDirectionNoArgShowsOptions() {
        zhuJiPlayer(Gender.MALE);
        String result = directionCommand.execute(ctx());
        assertTrue(result.contains("剑修"), result);
        assertTrue(result.contains("法修"), result);
        assertTrue(result.contains("医修"), result);
    }

    // === 方向属性加成 ===

    @Test
    void swordBonusAttack() {
        Player p = new Player("u1");
        p.setLevel(21);
        p.setGender(Gender.MALE);
        p.setDirection(CultivationDirection.SWORD);
        Attributes base = Attributes.of(new Player("u2") {{
            setLevel(21);
            setGender(Gender.MALE);
        }});
        Attributes withDir = Attributes.of(p);
        assertEquals(Math.round(base.attack() * 1.15), withDir.attack());
    }

    @Test
    void magicBonusSense() {
        Player p = new Player("u1");
        p.setLevel(21);
        p.setGender(Gender.FEMALE);
        p.setDirection(CultivationDirection.MAGIC);
        Attributes base = Attributes.of(new Player("u2") {{
            setLevel(21);
            setGender(Gender.FEMALE);
        }});
        Attributes withDir = Attributes.of(p);
        assertEquals(Math.round(base.sense() * 1.15), withDir.sense());
    }

    @Test
    void medicineBonusHp() {
        Player p = new Player("u1");
        p.setLevel(21);
        p.setGender(Gender.MALE);
        p.setDirection(CultivationDirection.MEDICINE);
        Attributes base = Attributes.of(new Player("u2") {{
            setLevel(21);
            setGender(Gender.MALE);
        }});
        Attributes withDir = Attributes.of(p);
        assertEquals(Math.round(base.hp() * 1.10), withDir.hp());
    }

    // === 修炼效率加成 ===

    @Test
    void cultivationBonusValues() {
        assertEquals(1.0, CultivationDirection.SWORD.cultivationBonus(), 0.001);
        assertEquals(1.05, CultivationDirection.MAGIC.cultivationBonus(), 0.001);
        assertEquals(1.05, CultivationDirection.MEDICINE.cultivationBonus(), 0.001);
    }

    @Test
    void magicDirectionBoostsCultivation() {
        Player p = zhuJiPlayer(Gender.FEMALE);
        directionCommand.execute(ctx("法修"));
        p = playerService.find("u1").orElseThrow();
        long base = cultivationService.expectedReward(60, CultivationMethod.TU_NA, 21, 1.0);
        long boosted = cultivationService.expectedReward(60, CultivationMethod.TU_NA, 21, 1.0, 1.05);
        assertTrue(boosted > base);
    }

    // === 性别×方向联动功法 ===

    @Test
    void fenTianRequiresMale() {
        Player p = zhuJiPlayer(Gender.FEMALE);
        p.setDirection(CultivationDirection.SWORD);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.FEN_TIAN, 30);
        assertTrue(result.contains("男"), result);
    }

    @Test
    void fenTianRequiresSwordDirection() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setDirection(CultivationDirection.MAGIC);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.FEN_TIAN, 30);
        assertTrue(result.contains("剑修"), result);
    }

    @Test
    void fenTianWorksForMaleSword() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setDirection(CultivationDirection.SWORD);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.FEN_TIAN, 60);
        assertTrue(result.contains("焚天诀"), result);
        assertTrue(result.contains("修炼"), result);
    }

    @Test
    void jiuXiaoRequiresFemaleMagic() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setDirection(CultivationDirection.MAGIC);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.JIU_XIAO, 30);
        assertTrue(result.contains("女"), result);
    }

    @Test
    void danXinRequiresMedicineDirection() {
        Player p = zhuJiPlayer(Gender.MALE);
        p.setDirection(CultivationDirection.SWORD);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.DAN_XIN, 30);
        assertTrue(result.contains("医修"), result);
    }

    @Test
    void bingPoRequiresJinDan() {
        Player p = zhuJiPlayer(Gender.FEMALE);
        p.setDirection(CultivationDirection.SWORD);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.BING_PO, 30);
        assertTrue(result.contains("金丹期"), result);
    }

    @Test
    void bingPoWorksForFemaleSwordJinDan() {
        Player p = jinDanPlayer(Gender.FEMALE);
        p.setDirection(CultivationDirection.SWORD);
        playerService.save(p);
        String result = cultivationService.start(p, CultivationMethod.BING_PO, 60);
        assertTrue(result.contains("冰魄剑诀"), result);
    }

    @Test
    void linkedMethodHigherMultiplier() {
        // 焚天诀 1.4 > 紫府诀 1.15
        long tuNa = cultivationService.expectedReward(60, CultivationMethod.TU_NA, 21);
        long fenTian = cultivationService.expectedReward(60, CultivationMethod.FEN_TIAN, 21);
        assertTrue(fenTian > tuNa);
    }

    // === 加速丹 ===

    @Test
    void speedFinishCompletesCultivation() {
        Player p = zhuJiPlayer(Gender.MALE);
        cultivationService.start(p, CultivationMethod.TU_NA, 120); // 2h → +400 修为
        String result = cultivationService.speedFinish(playerService.find("u1").orElseThrow());
        assertTrue(result.contains("加速丹"), result);
        assertTrue(result.contains("+400"), result);
        assertTrue(cultivationService.activeTask("u1").isEmpty());
    }

    @Test
    void speedFinishWithoutTask() {
        zhuJiPlayer(Gender.MALE);
        String result = cultivationService.speedFinish(playerService.find("u1").orElseThrow());
        assertTrue(result.contains("没有进行中"), result);
    }
}

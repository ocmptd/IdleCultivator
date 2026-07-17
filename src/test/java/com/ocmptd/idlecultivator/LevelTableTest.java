package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.player.Attributes;
import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.LevelTable;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.Realm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelTableTest {

    @Test
    void realmByStage() {
        assertEquals(Realm.LIAN_QI, LevelTable.realmOf(1));
        assertEquals(Realm.LIAN_QI, LevelTable.realmOf(20));
        assertEquals(Realm.ZHU_JI, LevelTable.realmOf(21));
        assertEquals(Realm.JIN_DAN, LevelTable.realmOf(41));
        assertEquals(Realm.YUAN_YING, LevelTable.realmOf(61));
        assertEquals(Realm.YUAN_YING, LevelTable.realmOf(80));
    }

    @Test
    void breakthroughGates() {
        assertTrue(LevelTable.atBreakthrough(20));
        assertTrue(LevelTable.atBreakthrough(40));
        assertTrue(LevelTable.atBreakthrough(60));
        assertFalse(LevelTable.atBreakthrough(1));
        assertFalse(LevelTable.atBreakthrough(21));
        assertFalse(LevelTable.atBreakthrough(80)); // 满级不再突破
    }

    @Test
    void stageTotalsMatchPacing() {
        // 日均约 2000 修为:练气 ~2 天,筑基 ~20 天,金丹+元婴 ~40 天
        assertEquals(4100, totalExp(1, 20));
        long zhuJi = totalExp(21, 40);
        assertTrue(zhuJi >= 38_000 && zhuJi <= 44_000, "筑基总量: " + zhuJi);
        long late = totalExp(41, 80);
        assertTrue(late >= 110_000 && late <= 130_000, "后期总量: " + late);
    }

    private long totalExp(int from, int to) {
        long sum = 0;
        for (int lv = from; lv <= to && lv < LevelTable.MAX_LEVEL; lv++) {
            sum += LevelTable.expToNextLevel(lv);
        }
        return sum;
    }

    @Test
    void autoLevelUpStopsAtStageGate() {
        Player p = new Player("u1");
        p.setLevel(1);
        p.setExp(10_000); // 足够升满练气,但要卡在 20 级
        int gained = LevelTable.levelUp(p);
        assertEquals(19, gained);
        assertEquals(20, p.level());
        assertTrue(p.exp() > 0);
        assertEquals(Realm.LIAN_QI, p.realm());
    }

    @Test
    void yuanYingSuppression() {
        assertEquals(1.0, LevelTable.cultivationMultiplier(1));
        assertEquals(1.0, LevelTable.cultivationMultiplier(60));
        assertEquals(0.8, LevelTable.cultivationMultiplier(61));
    }

    @Test
    void attributesGrowWithLevelAndStage() {
        Player p = new Player("u1");
        p.setLevel(1);
        Attributes lv1 = Attributes.of(p);
        assertEquals(100, lv1.hp());
        assertEquals(10, lv1.attack());
        assertEquals(5, lv1.defense());
        assertEquals(10, lv1.sense());

        p.setLevel(21); // 进入筑基,阶段加成
        Attributes lv21 = Attributes.of(p);
        assertEquals(100 + 20 * 20, lv21.hp());
        assertEquals(10 + 3 * 20 + 20, lv21.attack());
        assertEquals(5 + 2 * 20 + 15, lv21.defense());

        p.setGender(Gender.MALE);
        Attributes male = Attributes.of(p);
        assertEquals(Math.round(lv21.hp() * 1.05), male.hp());
    }

    @Test
    void legacyRealmConversion() {
        assertEquals(1, LevelTable.initialLevelOf(Realm.LIAN_QI));
        assertEquals(21, LevelTable.initialLevelOf(Realm.ZHU_JI));
        assertEquals(41, LevelTable.initialLevelOf(Realm.JIN_DAN));
        assertEquals(61, LevelTable.initialLevelOf(Realm.YUAN_YING));
        assertEquals(61, LevelTable.initialLevelOf(Realm.HUA_SHEN));
    }
}

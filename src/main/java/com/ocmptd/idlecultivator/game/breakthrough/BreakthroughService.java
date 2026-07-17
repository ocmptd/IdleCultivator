package com.ocmptd.idlecultivator.game.breakthrough;

import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.LevelTable;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 突破系统:阶段末(20/40/60 级)且修为足够时可突破进入下一阶段,基础成功率 80%;
 * 可消耗突破丹药(如筑基丹)将成功率提升至 95%。
 */
public class BreakthroughService {
    public static final double BASE_SUCCESS_RATE = 0.8;
    public static final double PILL_SUCCESS_RATE = 0.95;
    public static final String PILL_NAME = "筑基丹";

    private final PlayerService playerService;

    public BreakthroughService(PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * 尝试突破。
     *
     * @param usePill 是否消耗突破丹药(背包需有 {@link #PILL_NAME})
     */
    public String attempt(Player player, boolean usePill) {
        if (player.level() >= LevelTable.MAX_LEVEL) {
            return "道友已至 Lv." + LevelTable.MAX_LEVEL + " 满级,此界之内再无可突破之境。";
        }
        if (!LevelTable.atBreakthrough(player.level())) {
            return "当前 Lv." + player.level() + " 无需突破,修为攒够会自动升级(每 20 级阶段末需突破)。";
        }
        long cost = LevelTable.expToNextLevel(player.level());
        if (player.exp() < cost) {
            return "修为不足(" + player.exp() + "/" + cost + "),请继续修炼。";
        }
        double rate = BASE_SUCCESS_RATE;
        String pillNote = "";
        if (usePill) {
            String newInventory = Inventory.consume(player.inventory(), PILL_NAME, 1);
            if (newInventory == null) {
                return "背包中没有【" + PILL_NAME + "】,无法用丹突破。";
            }
            player.setInventory(newInventory);
            rate = PILL_SUCCESS_RATE;
            pillNote = "服下【" + PILL_NAME + "】,成功率提升至 95%!";
        }
        boolean success = ThreadLocalRandom.current().nextDouble() < rate;
        String result;
        if (success) {
            player.setExp(player.exp() - cost);
            player.setLevel(player.level() + 1);
            player.setRealm(LevelTable.realmOf(player.level()));
            LevelTable.levelUp(player);
            result = "突破成功!道友已踏入" + player.realm().label()
                    + ",升至 Lv." + player.level() + "!";
        } else {
            result = "突破失败,道友气息紊乱,还需稳固修为再试。";
        }
        playerService.save(player);
        return pillNote.isEmpty() ? result : pillNote + "\n" + result;
    }
}

package com.ocmptd.idlecultivator.game.breakthrough;

import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.player.Realm;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 突破系统:修为足够时可突破,基础成功率 80%;
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
        Realm current = player.realm();
        if (current.next() == current) {
            return "道友已至" + current.label() + ",此界之内再无可突破之境。";
        }
        if (player.exp() < current.expToNext()) {
            return "修为不足(" + player.exp() + "/" + current.expToNext() + "),请继续修炼。";
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
            player.setExp(player.exp() - current.expToNext());
            player.setRealm(current.next());
            result = "突破成功!道友已踏入" + player.realm().label() + "!";
        } else {
            result = "突破失败,道友气息紊乱,还需稳固修为再试。";
        }
        playerService.save(player);
        return pillNote.isEmpty() ? result : pillNote + "\n" + result;
    }
}

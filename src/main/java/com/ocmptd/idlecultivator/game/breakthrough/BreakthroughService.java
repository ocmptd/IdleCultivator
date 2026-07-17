package com.ocmptd.idlecultivator.game.breakthrough;

import com.ocmptd.idlecultivator.game.player.Player;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.game.player.Realm;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 突破系统(Phase 0 骨架):修为足够时可突破,基础成功率 80%。
 * 丹药提升成功率在 Phase 1 完善。
 */
public class BreakthroughService {
    public static final double BASE_SUCCESS_RATE = 0.8;

    private final PlayerService playerService;

    public BreakthroughService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public String attempt(Player player) {
        Realm current = player.realm();
        if (current.next() == current) {
            return "道友已至" + current.label() + ",此界之内再无可突破之境。";
        }
        if (player.exp() < current.expToNext()) {
            return "修为不足(" + player.exp() + "/" + current.expToNext() + "),请继续修炼。";
        }
        boolean success = ThreadLocalRandom.current().nextDouble() < BASE_SUCCESS_RATE;
        if (success) {
            player.setExp(player.exp() - current.expToNext());
            player.setRealm(current.next());
            playerService.save(player);
            return "突破成功!道友已踏入" + player.realm().label() + "!";
        } else {
            playerService.save(player);
            return "突破失败,道友气息紊乱,还需稳固修为再试。";
        }
    }
}

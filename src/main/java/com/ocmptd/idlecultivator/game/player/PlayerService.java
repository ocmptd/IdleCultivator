package com.ocmptd.idlecultivator.game.player;

import com.ocmptd.idlecultivator.game.item.Inventory;
import com.ocmptd.idlecultivator.storage.PlayerRepository;

import java.util.Optional;

/**
 * 玩家角色服务。
 */
public class PlayerService {
    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public Optional<Player> find(String userId) {
        Optional<Player> opt = repository.findById(userId);
        opt.ifPresent(this::migrateLegacyLevel);
        return opt;
    }

    /** 老数据折算:无等级者按已有境界给初始等级。 */
    private void migrateLegacyLevel(Player p) {
        if (p.level() > 0) return;
        p.setLevel(LevelTable.initialLevelOf(p.realm()));
        LevelTable.levelUp(p);
        repository.save(p);
    }

    /**
     * 获得修为并自动升级(可连升,阶段末卡突破),已保存。
     * @return 升级提示(未升级返回空字符串)
     */
    public String gainExp(Player p, long amount) {
        p.addExp(amount);
        int gained = LevelTable.levelUp(p);
        repository.save(p);
        if (gained == 0) return "";
        String tip = "\n恭喜升至 Lv." + p.level() + "!";
        if (LevelTable.atBreakthrough(p.level())) {
            tip += "已至" + p.realm().label() + "圆满,需突破方可继续升级。";
        }
        return tip;
    }

    public Player create(String userId, String groupId, String name, Gender gender) {
        Player p = new Player(userId);
        p.setGroupId(groupId);
        p.setName(name);
        p.setGender(gender);
        p.setRealm(Realm.LIAN_QI);
        p.setLevel(1);
        p.setEquipment("木剑×1");
        p.setInventory(Inventory.add("", "筑基丹", 1));
        p.setCreatedAt(System.currentTimeMillis());
        repository.save(p);
        return p;
    }

    public void save(Player p) {
        repository.save(p);
    }

    /**
     * 状态描述(Phase 0 文字版形象)。
     */
    public String describe(Player p) {
        StringBuilder sb = new StringBuilder();
        sb.append("道友[").append(p.name() == null ? p.userId() : p.name()).append("]");
        if (p.gender() != null) sb.append("(").append(p.gender().label()).append(")");
        sb.append("\n✨等级:Lv.").append(p.level()).append("(").append(p.realm().label()).append(")");
        if (p.level() >= LevelTable.MAX_LEVEL) {
            sb.append("\n修为:").append(p.exp()).append("(已满级)");
        } else if (LevelTable.atBreakthrough(p.level())) {
            sb.append("\n修为:").append(p.exp()).append("/").append(LevelTable.expToNextLevel(p.level()))
                    .append("(需突破)");
        } else {
            sb.append("\n修为:").append(p.exp()).append("/").append(LevelTable.expToNextLevel(p.level()));
        }
        sb.append("\n").append(Attributes.of(p).display());
        sb.append("\n灵石:").append(p.spiritStones());
        if (p.direction() != null) sb.append("\n方向:").append(p.direction().label());
        sb.append("\n装备:").append(p.equipment().isEmpty() ? "无" : p.equipment());
        sb.append("\n背包:").append(Inventory.display(p.inventory()));
        return sb.toString();
    }
}

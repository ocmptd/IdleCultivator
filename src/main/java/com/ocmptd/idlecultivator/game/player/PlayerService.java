package com.ocmptd.idlecultivator.game.player;

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
        return repository.findById(userId);
    }

    public Player create(String userId, String groupId, String name, Gender gender) {
        Player p = new Player(userId);
        p.setGroupId(groupId);
        p.setName(name);
        p.setGender(gender);
        p.setRealm(Realm.LIAN_QI);
        p.setEquipment("木剑×1");
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
        sb.append("道友").append(p.name() == null ? p.userId() : p.name());
        if (p.gender() != null) sb.append("(").append(p.gender().label()).append(")");
        sb.append("\n✨境界:").append(p.realm().label())
                .append("(修为:").append(p.exp()).append("/").append(p.realm().expToNext()).append(")");
        sb.append("\n灵石:").append(p.spiritStones());
        if (p.direction() != null) sb.append("\n方向:").append(p.direction().label());
        sb.append("\n装备:").append(p.equipment().isEmpty() ? "无" : p.equipment());
        return sb.toString();
    }
}

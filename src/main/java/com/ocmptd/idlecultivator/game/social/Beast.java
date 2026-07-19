package com.ocmptd.idlecultivator.game.social;

/**
 * 异兽入侵:群内出现的妖兽,玩家可用 !击杀 攻击。
 */
public class Beast {
    private final String groupId;
    private final String name;
    private final int maxHp;
    private int hp;
    private final long expReward;
    private final long stoneReward;
    private final long spawnTime;

    public Beast(String groupId, String name, int maxHp, int hp, long expReward, long stoneReward, long spawnTime) {
        this.groupId = groupId;
        this.name = name;
        this.maxHp = maxHp;
        this.hp = hp;
        this.expReward = expReward;
        this.stoneReward = stoneReward;
        this.spawnTime = spawnTime;
    }

    public String groupId() {
        return groupId;
    }

    public String name() {
        return name;
    }

    public int maxHp() {
        return maxHp;
    }

    public int hp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public long expReward() {
        return expReward;
    }

    public long stoneReward() {
        return stoneReward;
    }

    public long spawnTime() {
        return spawnTime;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}

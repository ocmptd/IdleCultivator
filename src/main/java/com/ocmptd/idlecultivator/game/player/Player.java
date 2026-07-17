package com.ocmptd.idlecultivator.game.player;

/**
 * 玩家角色。
 */
public class Player {
    private final String userId;
    private String groupId;
    private String name;
    private Gender gender;
    private Realm realm = Realm.LIAN_QI;
    private int level = 1;
    private int season = 1;
    private long exp;
    private long spiritStones;
    private String equipment = "";
    private String inventory = "";
    private CultivationDirection direction;
    private int activeStreak;
    private long nameChangedAt;
    private long createdAt;

    public Player(String userId) {
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }

    public String groupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender gender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Realm realm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int season() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public long exp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public void addExp(long delta) {
        this.exp += delta;
    }

    public long spiritStones() {
        return spiritStones;
    }

    public void setSpiritStones(long spiritStones) {
        this.spiritStones = spiritStones;
    }

    public void addSpiritStones(long delta) {
        this.spiritStones += delta;
    }

    public String equipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String inventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    public CultivationDirection direction() {
        return direction;
    }

    public void setDirection(CultivationDirection direction) {
        this.direction = direction;
    }

    public int activeStreak() {
        return activeStreak;
    }

    public void setActiveStreak(int activeStreak) {
        this.activeStreak = activeStreak;
    }

    public long nameChangedAt() {
        return nameChangedAt;
    }

    public void setNameChangedAt(long nameChangedAt) {
        this.nameChangedAt = nameChangedAt;
    }

    public long createdAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

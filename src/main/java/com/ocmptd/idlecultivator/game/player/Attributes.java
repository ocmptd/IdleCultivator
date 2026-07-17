package com.ocmptd.idlecultivator.game.player;

/**
 * 角色属性,由等级/阶段/性别实时推导,不落库(便于赛季重置与调参)。
 * 气血 100+20/级;攻击 10+3/级(每阶段额外+20);防御 5+2/级(每阶段额外+15);神识 10+2/级。
 * 性别加成:男 气血/防御 +5%;女 神识 +5%。
 */
public record Attributes(long hp, long attack, long defense, long sense) {

    public static Attributes of(Player p) {
        int level = Math.max(1, p.level());
        int stage = (level - 1) / LevelTable.STAGE_SIZE;
        long hp = 100 + 20L * (level - 1);
        long attack = 10 + 3L * (level - 1) + 20L * stage;
        long defense = 5 + 2L * (level - 1) + 15L * stage;
        long sense = 10 + 2L * (level - 1);
        if (p.gender() == Gender.MALE) {
            hp = Math.round(hp * 1.05);
            defense = Math.round(defense * 1.05);
        } else if (p.gender() == Gender.FEMALE) {
            sense = Math.round(sense * 1.05);
        }
        return new Attributes(hp, attack, defense, sense);
    }

    public String display() {
        return "气血:" + hp + " 攻击:" + attack + " 防御:" + defense + " 神识:" + sense;
    }
}

package com.ocmptd.idlecultivator.game.portrait;

import com.ocmptd.idlecultivator.game.player.Gender;
import com.ocmptd.idlecultivator.game.player.Player;

/**
 * 形象描述服务:根据角色性别/境界/方向/装备生成
 * 1) 群内展示用的中文姿容描述
 * 2) AI 绘图用的图片描述词(prompt),当前仅展示,后续接入图片生成直接复用
 */
public class PortraitService {

    /** 中文姿容描述(用于 状态/形象描述 指令) */
    public String describeAppearance(Player p) {
        return "姿容:" + appearancePrompt(p);
    }

    /** 中文姿容描述正文(不含展示用前缀)。 */
    public String appearancePrompt(Player p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.gender() == Gender.FEMALE ? "女青丝如瀑" : "男黑发束起");
        sb.append(",").append(switch (p.realm()) {
            case LIAN_QI -> "身着麻衣布鞋";
            case ZHU_JI -> "身着道袍长衫";
            case JIN_DAN -> "身着道纹法袍";
            case YUAN_YING -> "仙风道骨,衣袂飘飘";
            case HUA_SHEN -> "周身灵光隐现";
            case DA_CHENG -> "气息渊渟岳峙,恍若谪仙";
        });
        if (p.direction() != null) {
            sb.append(",").append(switch (p.direction()) {
                case SWORD -> "腰间悬着一柄寒光凛凛的长剑";
                case MAGIC -> "指尖萦绕着点点灵光";
                case MEDICINE -> "腰间挂着一个装满药草的药囊";
            });
        } else {
            sb.append(",手持一柄普通的木剑");
        }
        return sb.toString();
    }

    /**
     * 图片描述词(AI 绘图 prompt)。暂不实际生成图片。
     */
    public String imagePrompt(Player p) {
        StringBuilder sb = new StringBuilder("中国古风修仙人物立绘,");
        sb.append(p.gender() == Gender.FEMALE ? "女性修士,青丝长发," : "男性修士,黑发束冠,");
        sb.append(switch (p.realm()) {
            case LIAN_QI -> "练气期新人,粗布麻衣,";
            case ZHU_JI -> "筑基期修士,青色道袍,";
            case JIN_DAN -> "金丹期修士,绣有道纹的法袍,丹田金光,";
            case YUAN_YING -> "元婴期高人,华贵法衣,身后元婴虚影,";
            case HUA_SHEN -> "化神期大能,周身灵气环绕,";
            case DA_CHENG -> "大乘期仙人,脚踏祥云,仙气缭绕,";
        });
        if (p.direction() != null) {
            sb.append(switch (p.direction()) {
                case SWORD -> "剑修,手持仙剑,剑气纵横,";
                case MAGIC -> "法修,掌心法阵光辉,符箓环绕,";
                case MEDICINE -> "医修,手持药鼎,药草与丹药相伴,";
            });
        }
        sb.append("云雾缭绕的仙山背景,水墨画风格,高清细节");
        return sb.toString();
    }

    /** 道具的图片描述词。 */
    public String itemImagePrompt(String itemName) {
        String feature = switch (itemName) {
            case "灵尘" -> "一撮散发微光的银色尘埃,悬浮于掌心";
            case "残破法宝" -> "一件布满裂纹的古旧法器,残余灵光明灭";
            case "筑基丹" -> "一枚温润如玉的丹药,表面丹纹流转,盛于玉瓶之中";
            case "木剑" -> "一柄朴实无华的木质长剑,剑身刻有浅浅符文";
            default -> "一件散发灵气的修仙道具「" + itemName + "」";
        };
        return "中国古风修仙物品特写," + feature + ",仙气缭绕,水墨画风格,高清细节";
    }
}

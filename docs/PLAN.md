# IdleCultivator 修仙挂机游戏 — 开发规划

> 基于《修仙挂机游戏优化》设计方案 (2026/7/16)
> 技术选型:Java (JDK 21) + Maven 单体架构,QQ 机器人框架 [qqpd-bot-java](https://github.com/Kloping/qqpd-bot-java)

## 一、总体架构

```
IdleCultivator (单体)
├── bot        —— qqpd-bot-java 接入层:Starter 启动、事件监听、消息收发
├── command    —— 指令路由框架:解析 "!xxx" 指令并分发到各 handler
├── game       —— 游戏核心逻辑
│   ├── player       玩家/角色(性别、境界、属性、修炼方向、形象)
│   ├── cultivation  修炼系统(双轨制、溢出惩罚、自动结算)
│   ├── breakthrough 突破系统(成功率、丹药加成)
│   ├── item         道具系统(功法/法宝/丹药/服饰/配饰/材料…)
│   ├── social       社交系统(群活跃度、互助、道侣、宗门战)
│   └── event        游戏事件(灵气潮汐、异兽入侵、走火入魔)
├── scheduler  —— 定时任务(修炼结算检查、群活跃度更新、每日灵气潮汐)
├── storage    —— 持久化层(SQLite:users / cultivation_tasks / group_status …)
└── config     —— 配置管理(appid / token / secret、游戏参数)
```

## 二、分阶段计划

### Phase 0:基础框架搭建 ✅(本次 PR)
- [x] Maven 项目初始化(JDK 21、编码、编译插件、打包插件)
- [x] 集成 qqpd-bot-java (`io.github.kloping:bot-qqpd-java`)
- [x] 应用启动入口 + 配置加载(`config.properties`)
- [x] 指令路由框架(`CommandRouter` + `Command` 接口,注解式注册)
- [x] SQLite 持久化骨架(建表:users / cultivation_tasks / group_status)
- [x] 定时任务骨架(修炼任务结算轮询、群活跃度小时更新)
- [x] 占位指令:`!帮助`、`!创建角色`、`!修炼`、`!突破`、`!状态`

### Phase 1:核心修炼循环
- [ ] `!创建角色`:性别选择、初始属性(男:体魄+5% / 女:神识+5%)、称号
- [ ] `!修炼 [功法] [时长]`:双轨制修炼
  - 快速修炼(默认 30 分钟,+100 修为 +50 灵石,无溢出)
  - 长时修炼(2-24h,时长×基础收益,超时每小时收益×0.7 衰减)
- [ ] 修炼自动结算:定时检查任务表,完成时机器人主动推送结果
- [ ] 溢出惩罚分级:≤1h→灵尘(×0.3);1-3h→残破法宝(×0.5);>3h→灵石(×0.7)+20% 走火入魔
- [ ] `!突破`:修为足够触发,可消耗丹药提升成功率
- [ ] `!状态`:境界、属性、进行中修炼、背包、文字形象描述

### Phase 2:养成与个性化
- [ ] 修炼方向系统:`!选择方向 剑修/法修/医修`(筑基期后),方向专属加成与技能
- [ ] 性别×方向联动功法(焚天诀 / 九霄云衣诀 / 丹心诀 / 冰魄剑诀)
- [ ] 道具系统:功法、法宝、丹药、服饰、配饰、材料、加速丹、溢出保护符等
- [ ] 形象定制:`!换发型 / !换服饰 / !换配饰`,境界解锁进阶外观
- [ ] 形象展示:状态文字描述,后续可选图片生成(`!展示形象`)

### Phase 3:社交与群联动
- [ ] 群活跃度分级(灵气稀薄/平和/充沛/鼎盛 → 效率 ×0.8/1.0/1.2/1.5),`!群状态`
- [ ] `!互助 @某人`:双方效率+20%/1h,最多叠 3 次
- [ ] 每日灵气潮汐(晚 8 点效率翻倍)、异兽入侵事件(`!击杀`)
- [ ] 宗门战:以 QQ 群为单位每周比拼总修为
- [ ] 社交惩罚:连续不发言效率递减,回归任务
- [ ] 道侣/情缘系统

### Phase 4:粘度系统与运营
- [ ] 每日机缘(`!机缘`)、晨课/晚课签到
- [ ] 周常试炼、专属称号/头衔
- [ ] 成就系统、排行榜
- [ ] 赛季/大版本机制(飞升篇:重置进度保留外观称号)
- [ ] 交易系统、炼丹(`!炼丹`)、炼器(`!锻造`)、秘境探索

## 三、数据库设计(SQLite)

- `users`:user_id, group_id, gender, level, current_exp, spirit_stones, equipment, inventory, cultivation_direction, active_streak, created_at
- `cultivation_tasks`:task_id, user_id, group_id, method, start_time, duration_minutes, status(0进行中/1已完成/2已过期), expected_reward, overflow_penalty
- `group_status`:group_id, last_active_time, message_count, cultivation_bonus, next_bonus_time

## 四、运行方式

1. 到 https://q.qq.com/ 申请机器人,获取 appid / token / secret
2. 复制 `config.properties.example` 为 `config.properties` 填入凭据
3. `mvn package` 后运行 `java -jar target/idle-cultivator-*-jar-with-dependencies.jar`

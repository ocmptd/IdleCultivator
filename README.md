# IdleCultivator 修仙挂机

QQ 群修仙挂机游戏机器人。指令极简(`!修炼` → `!突破`),默认 30 分钟自动完成修炼,群活跃度影响修炼效率,深度融合 QQ 群社交场景。

- 技术栈:Java 21 + Maven 单体架构
- 机器人框架:[qqpd-bot-java](https://github.com/Kloping/qqpd-bot-java)
- 持久化:SQLite
- 开发规划:见 [docs/PLAN.md](docs/PLAN.md)

## 快速开始

### 1. 申请机器人凭据

到 <https://q.qq.com/> 申请机器人,获取 `appid` / `token` / `secret`。

### 2. 配置

```bash
cp config.properties.example config.properties
# 编辑 config.properties 填入 appid / token / secret
```

### 3. 构建与运行

```bash
mvn package
java -jar target/idle-cultivator-jar-with-dependencies.jar
```

未配置凭据时,程序会以**本地控制台模式**启动,可直接在终端输入指令调试游戏逻辑:

```
> !创建角色
> !修炼
> !状态
```

## 指令一览(Phase 1)

| 指令 | 说明 |
| --- | --- |
| `!帮助` | 查看指令列表 |
| `!创建角色 [男/女] [道号]` | 创建修仙角色(赠筑基丹×1) |
| `!修炼 [功法] [时长]` | 开始修炼(默认 18~28 分钟随机);≤30 分钟为快速修炼自动收获,更长时长需手动收获;修炼中重复输入提示剩余分钟 |
| `!收获` | 收获修炼;满 10 分钟可提前结束(收益按进度衰减,越接近完成扣得越少);超时溢出按分级惩罚转化(≤1h 灵尘 / 1-3h 残破法宝 / >3h 灵石+20% 走火入魔) |
| `!功法` | 查看内置功法及收益倍率(吐纳诀/紫府诀/玄天功/太虚剑意,按境界解锁) |
| `!背包` | 查看背包物品、灵石与装备 |
| `!突破 [用丹]` | 修为足够时突破境界(基础 80%,带「用丹」消耗筑基丹提升至 95%) |
| `!状态` | 查看境界、属性、背包、姿容与修炼进度 |
| `!形象描述 [道具]` | 查看角色(或背包道具)的图片描述词(AI 绘图 prompt,暂不生成图片) |

指令前缀可在 `config.properties` 中通过 `command.prefix` 配置(可为空);修炼到期/结算结果会主动推送到所在群,无法推送时自动结算加经验(不会因此受溢出惩罚)。

## 项目结构

```
src/main/java/com/ocmptd/idlecultivator/
├── Application.java   启动入口
├── config/            配置加载
├── bot/               qqpd-bot-java 接入层
├── command/           指令路由框架与各指令实现
├── game/              游戏核心逻辑(player / cultivation / ...)
├── scheduler/         定时任务(修炼结算、群活跃度)
└── storage/           SQLite 持久化
```

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

## 指令一览(Phase 0)

| 指令 | 说明 |
| --- | --- |
| `!帮助` | 查看指令列表 |
| `!创建角色` | 创建修仙角色 |
| `!修炼 [功法] [时长]` | 开始修炼(默认 30 分钟) |
| `!突破` | 修为足够时突破境界 |
| `!状态` | 查看境界、属性与修炼进度 |

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

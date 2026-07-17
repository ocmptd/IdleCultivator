package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.bot.BotService;
import com.ocmptd.idlecultivator.command.CommandRouter;
import com.ocmptd.idlecultivator.command.commands.BreakthroughCommand;
import com.ocmptd.idlecultivator.command.commands.CreateRoleCommand;
import com.ocmptd.idlecultivator.command.commands.CultivateCommand;
import com.ocmptd.idlecultivator.command.commands.HelpCommand;
import com.ocmptd.idlecultivator.command.commands.StatusCommand;
import com.ocmptd.idlecultivator.config.BotConfig;
import com.ocmptd.idlecultivator.game.breakthrough.BreakthroughService;
import com.ocmptd.idlecultivator.game.cultivation.CultivationService;
import com.ocmptd.idlecultivator.game.player.PlayerService;
import com.ocmptd.idlecultivator.scheduler.GameScheduler;
import com.ocmptd.idlecultivator.storage.CultivationTaskRepository;
import com.ocmptd.idlecultivator.storage.Database;
import com.ocmptd.idlecultivator.storage.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * IdleCultivator 修仙挂机 启动入口。
 * 已配置 QQ 机器人凭据时接入 QQ 群;否则进入本地控制台模式便于调试。
 */
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        BotConfig config = BotConfig.load(Path.of("config.properties"));

        Database db = new Database(config.dbPath());
        PlayerService playerService = new PlayerService(new PlayerRepository(db));
        CultivationService cultivationService =
                new CultivationService(new CultivationTaskRepository(db), playerService);
        BreakthroughService breakthroughService = new BreakthroughService(playerService);

        CommandRouter router = new CommandRouter();
        router.register(new HelpCommand(router));
        router.register(new CreateRoleCommand(playerService));
        router.register(new CultivateCommand(playerService, cultivationService));
        router.register(new BreakthroughCommand(playerService, breakthroughService));
        router.register(new StatusCommand(playerService, cultivationService));

        GameScheduler scheduler = new GameScheduler(cultivationService);
        scheduler.start();

        if (config.hasCredentials()) {
            new BotService(config, router).start();
        } else {
            log.warn("未配置 bot.appid / bot.token,进入本地控制台模式(复制 config.properties.example 为 config.properties 可接入 QQ)");
            runConsole(router);
        }
    }

    /** 本地控制台模式:模拟单玩家发送指令,便于无凭据调试游戏逻辑。 */
    private static void runConsole(CommandRouter router) {
        System.out.println("=== IdleCultivator 控制台模式(输入 !帮助 查看指令,exit 退出)===");
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("exit")) break;
                String reply = router.handle("console-user", "console-group", line);
                if (reply != null) System.out.println(reply);
            }
        }
    }
}

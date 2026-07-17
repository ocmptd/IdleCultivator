package com.ocmptd.idlecultivator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 应用配置,从工作目录 config.properties 加载。
 */
public class BotConfig {
    private final String appId;
    private final String token;
    private final String secret;
    private final boolean sandbox;
    private final String dbPath;
    private final String imageCacheDir;
    private final String commandPrefix;
    private final boolean requireAt;

    private BotConfig(Properties props) {
        this.appId = props.getProperty("bot.appid", "").trim();
        this.token = props.getProperty("bot.token", "").trim();
        this.secret = props.getProperty("bot.secret", "").trim();
        this.sandbox = Boolean.parseBoolean(props.getProperty("bot.sandbox", "false").trim());
        this.dbPath = props.getProperty("db.path", "idle-cultivator.db").trim();
        this.imageCacheDir = props.getProperty("image.cache.dir", "image-cache").trim();
        this.commandPrefix = props.getProperty("command.prefix", "!").trim();
        this.requireAt = Boolean.parseBoolean(props.getProperty("command.require-at", "false").trim());
    }

    public static BotConfig load(Path path) throws IOException {
        Properties props = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            }
        }
        return new BotConfig(props);
    }

    public boolean hasCredentials() {
        return !appId.isEmpty() && !token.isEmpty();
    }

    public String appId() {
        return appId;
    }

    public String token() {
        return token;
    }

    public String secret() {
        return secret;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String dbPath() {
        return dbPath;
    }

    public String imageCacheDir() {
        return imageCacheDir;
    }

    /** 指令前缀,可为空字符串(无前缀) */
    public String commandPrefix() {
        return commandPrefix;
    }

    /** 群消息是否仅在 @ 机器人时触发 */
    public boolean requireAt() {
        return requireAt;
    }
}

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
    private final String imageApiUrl;
    private final String imageApiKey;
    private final String imageApiModel;
    private final String imageApiSize;
    private final String imageApiImageSize;
    private final String imageApiPromptPrefix;
    private final String commandPrefix;
    private final boolean requireAt;

    private BotConfig(Properties props) {
        this.appId = props.getProperty("bot.appid", "").trim();
        this.token = props.getProperty("bot.token", "").trim();
        this.secret = props.getProperty("bot.secret", "").trim();
        this.sandbox = Boolean.parseBoolean(props.getProperty("bot.sandbox", "false").trim());
        this.dbPath = props.getProperty("db.path", "idle-cultivator.db").trim();
        this.imageCacheDir = props.getProperty("image.cache.dir", "image-cache").trim();
        this.imageApiUrl = props.getProperty("image.api.url", "https://www.right.codes").trim();
        this.imageApiKey = props.getProperty("image.api.key", "").trim();
        this.imageApiModel = props.getProperty("image.api.model", "gpt-image-2").trim();
        this.imageApiSize = props.getProperty("image.api.size", "1:1").trim();
        this.imageApiImageSize = props.getProperty("image.api.image-size", "1K").trim();
        this.imageApiPromptPrefix = props.getProperty("image.api.prompt-prefix",
                "Idle Cultivator 指尖修仙 左上角水印.游戏素材,不要有其他无关元素").trim();
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

    public String imageApiUrl() {
        return imageApiUrl;
    }

    public String imageApiKey() {
        return imageApiKey;
    }

    public String imageApiModel() {
        return imageApiModel;
    }

    public String imageApiSize() {
        return imageApiSize;
    }

    public String imageApiImageSize() {
        return imageApiImageSize;
    }

    public String imageApiPromptPrefix() {
        return imageApiPromptPrefix;
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

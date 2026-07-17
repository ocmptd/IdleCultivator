package com.ocmptd.idlecultivator.game.image;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ocmptd.idlecultivator.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * right.codes 异步图片生成器。
 * 提交任务后轮询结果并将图片保存到图片缓存目录。
 */
public class RightCodesImageGenerator implements ImageCacheService.ImageGenerator {
    private static final Logger log = LoggerFactory.getLogger(RightCodesImageGenerator.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String size;
    private final String imageSize;
    private final String promptPrefix;
    private final Path cacheDir;

    public RightCodesImageGenerator(BotConfig config, Path cacheDir) {
        this.baseUrl = config.imageApiUrl().replaceAll("/+$", "");
        this.apiKey = config.imageApiKey();
        this.model = config.imageApiModel();
        this.size = config.imageApiSize();
        this.imageSize = config.imageApiImageSize();
        this.promptPrefix = config.imageApiPromptPrefix();
        this.cacheDir = cacheDir.toAbsolutePath().normalize();
    }

    @Override
    public Optional<Path> generate(String prompt) {
        try {
            String taskId = submit(prompt);
            String imageUrl = poll(taskId);
            if (imageUrl == null) return Optional.empty();
            return Optional.of(download(imageUrl, prompt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("图片生成被中断");
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            log.warn("图片生成失败", e);
            return Optional.empty();
        }
    }

    private String submit(String prompt) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", promptPrefix + prompt);
        body.addProperty("n", 1);
        body.addProperty("size", size);
        body.addProperty("imageSize", imageSize);
        body.addProperty("async", true);

        HttpRequest request = requestBuilder(baseUrl + "/draw/v1/images/generations")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response, "提交图片生成任务");

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String taskId = stringValue(json, "task_id");
        if (taskId == null) taskId = stringValue(json, "id");
        if (taskId == null || taskId.isBlank()) {
            throw new IOException("生成任务响应缺少 task_id/id");
        }
        return taskId;
    }

    private String poll(String taskId) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TOTAL_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            HttpRequest request = requestBuilder(baseUrl + "/v1/tasks/" + taskId)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "查询图片生成任务");

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String imageUrl = imageUrl(json);
            if (imageUrl != null) return imageUrl;

            String status = stringValue(json, "status");
            if (status != null && switch (status.toLowerCase()) {
                case "failed", "error", "cancelled", "canceled" -> true;
                default -> false;
            }) {
                throw new IOException("图片生成任务失败,status=" + status);
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IOException("图片生成任务超时");
    }

    private Path download(String imageUrl, String prompt) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(response, "下载生成图片");

        Files.createDirectories(cacheDir);
        Path imagePath = cacheDir.resolve(sha256Prefix(prompt) + ".png").toAbsolutePath();
        Files.write(imagePath, response.body());
        return imagePath;
    }

    private HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey);
    }

    private static void ensureSuccess(HttpResponse<?> response, String operation) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(operation + "失败,http status=" + response.statusCode());
        }
    }

    private static String stringValue(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value != null && !value.isJsonNull() && value.isJsonPrimitive()
                ? value.getAsString() : null;
    }

    private static String imageUrl(JsonObject json) {
        JsonElement dataElement = json.get("data");
        if (dataElement == null || !dataElement.isJsonArray()) return null;
        JsonArray data = dataElement.getAsJsonArray();
        if (data.isEmpty() || !data.get(0).isJsonObject()) return null;
        return stringValue(data.get(0).getAsJsonObject(), "url");
    }

    private static String sha256Prefix(String prompt) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少 SHA-256", e);
        }
    }
}

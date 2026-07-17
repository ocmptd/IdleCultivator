package com.ocmptd.idlecultivator.game.image;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 图片缓存:以 prompt 对应缓存目录下的相对图片路径。
 * AI 生图 API 后续通过 setGenerator 注入。
 */
public class ImageCacheService {
    private static final Logger log = LoggerFactory.getLogger(ImageCacheService.class);
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private final Path cacheDir;
    private final Path mapFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> mappings = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "image-cache-generator");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private volatile ImageGenerator generator;

    public enum AsyncState {
        HIT,
        GENERATING,
        DISABLED
    }

    public record AsyncLookup(AsyncState state, Path path) {
        public static AsyncLookup hit(Path path) {
            return new AsyncLookup(AsyncState.HIT, path);
        }

        public static AsyncLookup generating() {
            return new AsyncLookup(AsyncState.GENERATING, null);
        }

        public static AsyncLookup disabled() {
            return new AsyncLookup(AsyncState.DISABLED, null);
        }
    }

    @FunctionalInterface
    public interface ImageGenerator {
        Optional<Path> generate(String prompt);
    }

    public ImageCacheService(Path cacheDir) {
        this.cacheDir = cacheDir.toAbsolutePath().normalize();
        this.mapFile = this.cacheDir.resolve("map.json");
        load();
    }

    public Optional<Path> lookup(String prompt) {
        String relativePath = mappings.get(prompt);
        if (relativePath == null) return Optional.empty();
        Path imagePath = cacheDir.resolve(relativePath).normalize();
        if (Files.isRegularFile(imagePath)) return Optional.of(imagePath.toAbsolutePath());
        return Optional.empty();
    }

    public synchronized void put(String prompt, String relativePath) {
        mappings.put(prompt, relativePath);
        persist();
    }

    public void setGenerator(ImageGenerator generator) {
        this.generator = generator;
    }

    public Optional<Path> getOrGenerate(String prompt) {
        Optional<Path> cached = lookup(prompt);
        if (cached.isPresent()) return cached;

        ImageGenerator currentGenerator = generator;
        if (currentGenerator == null) return Optional.empty();

        Optional<Path> generated = currentGenerator.generate(prompt);
        if (generated.isEmpty()) return Optional.empty();
        Path generatedPath = generated.get().toAbsolutePath().normalize();
        put(prompt, cacheDir.relativize(generatedPath).toString());
        return Optional.of(generatedPath);
    }

    public AsyncLookup lookupOrGenerateAsync(String prompt) {
        Optional<Path> cached = lookup(prompt);
        if (cached.isPresent()) return AsyncLookup.hit(cached.get());

        if (generator == null) return AsyncLookup.disabled();
        if (inFlight.add(prompt)) {
            executor.submit(() -> {
                try {
                    getOrGenerate(prompt);
                } catch (RuntimeException e) {
                    log.warn("异步生成图片失败", e);
                } finally {
                    inFlight.remove(prompt);
                }
            });
        }
        return AsyncLookup.generating();
    }

    private void load() {
        if (!Files.exists(mapFile)) {
            log.warn("图片缓存映射不存在,使用空缓存: {}", mapFile);
            return;
        }
        try (Reader reader = Files.newBufferedReader(mapFile, StandardCharsets.UTF_8)) {
            Map<String, String> loaded = gson.fromJson(reader, MAP_TYPE);
            if (loaded != null) mappings.putAll(loaded);
        } catch (IOException | RuntimeException e) {
            log.warn("读取图片缓存映射失败: {}", mapFile, e);
            mappings.clear();
        }
    }

    private void persist() {
        try {
            Files.createDirectories(cacheDir);
            Map<String, String> snapshot = new HashMap<>(mappings);
            try (Writer writer = Files.newBufferedWriter(mapFile, StandardCharsets.UTF_8)) {
                gson.toJson(snapshot, MAP_TYPE, writer);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("写入图片缓存映射失败: {}", mapFile, e);
        }
    }
}

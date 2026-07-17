package com.ocmptd.idlecultivator;

import com.ocmptd.idlecultivator.game.image.ImageCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageCacheServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void lookupReturnsExistingCachedImage() throws Exception {
        Path image = Files.createFile(tempDir.resolve("portrait.png"));
        Files.writeString(tempDir.resolve("map.json"), "{\"prompt\":\"portrait.png\"}", StandardCharsets.UTF_8);

        ImageCacheService service = new ImageCacheService(tempDir);

        assertEquals(Optional.of(image.toAbsolutePath()), service.lookup("prompt"));
    }

    @Test
    void lookupReturnsEmptyWhenMappingOrFileIsMissing() throws Exception {
        Files.writeString(tempDir.resolve("map.json"),
                "{\"missing-mapping\":\"missing.png\",\"missing-file\":\"not-created.png\"}",
                StandardCharsets.UTF_8);

        ImageCacheService service = new ImageCacheService(tempDir);

        assertTrue(service.lookup("unknown").isEmpty());
        assertTrue(service.lookup("missing-file").isEmpty());
    }

    @Test
    void putPersistsMappingAndNewInstanceCanReadIt() throws Exception {
        Path image = Files.createFile(tempDir.resolve("item.jpg"));
        ImageCacheService service = new ImageCacheService(tempDir);

        service.put("item prompt", "item.jpg");

        assertTrue(Files.exists(tempDir.resolve("map.json")));
        assertTrue(Files.readString(tempDir.resolve("map.json")).contains("\"item prompt\""));
        assertEquals(Optional.of(image.toAbsolutePath()), new ImageCacheService(tempDir).lookup("item prompt"));
    }

    @Test
    void getOrGenerateReturnsEmptyWithoutGenerator() {
        ImageCacheService service = new ImageCacheService(tempDir);

        assertTrue(service.getOrGenerate("prompt").isEmpty());
    }

    @Test
    void getOrGenerateUsesGeneratorAndPersistsResult() throws Exception {
        ImageCacheService service = new ImageCacheService(tempDir);
        Path image = tempDir.resolve("generated.png");
        service.setGenerator(prompt -> {
            try {
                Files.createFile(image);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Optional.of(image);
        });

        assertEquals(Optional.of(image.toAbsolutePath()), service.getOrGenerate("generated prompt"));
        assertEquals(Optional.of(image.toAbsolutePath()),
                new ImageCacheService(tempDir).lookup("generated prompt"));
        assertFalse(service.getOrGenerate("generated prompt").isEmpty());
    }
}

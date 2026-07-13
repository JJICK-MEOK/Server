package com.jjikmeok.app.global.infra.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void store_writesToExternalDirectoryAndReturnsLocalUrl() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setDirectory(tempDirectory.toString());
        properties.getLocal().setPublicBaseUrl("http://localhost:8080/uploads/");
        LocalFileStorageService service = new LocalFileStorageService(properties);

        String url = service.store("images/activities/kopis/id.png", new byte[]{1, 2, 3}, "image/png");

        assertThat(url).isEqualTo("http://localhost:8080/uploads/images/activities/kopis/id.png");
        assertThat(Files.readAllBytes(tempDirectory.resolve("images/activities/kopis/id.png")))
                .containsExactly(1, 2, 3);
    }
}

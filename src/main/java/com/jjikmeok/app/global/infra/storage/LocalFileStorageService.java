package com.jjikmeok.app.global.infra.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@Profile("!prod")
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class LocalFileStorageService implements StorageService {
    private final Path rootDirectory;
    private final String publicBaseUrl;

    public LocalFileStorageService(StorageProperties properties) {
        this.rootDirectory = Path.of(properties.getLocal().getDirectory()).toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(properties.getLocal().getPublicBaseUrl());
    }

    @Override
    public String store(String objectName, byte[] content, String contentType) {
        StorageInputValidator.validate(objectName, content, contentType);
        Path target = rootDirectory.resolve(objectName).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Object name escapes local storage directory");
        }

        try {
            Files.createDirectories(target.getParent());
            try {
                Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // Content-addressed names make an existing object equivalent to a successful upload.
            }
            return publicBaseUrl + "/" + objectName.replace('\\', '/');
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write object to local storage", e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Local storage public base URL must not be blank");
        }
        return value.replaceAll("/+$", "");
    }
}

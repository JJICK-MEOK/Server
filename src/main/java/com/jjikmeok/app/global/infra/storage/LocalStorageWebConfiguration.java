package com.jjikmeok.app.global.infra.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@Profile("!prod")
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class LocalStorageWebConfiguration implements WebMvcConfigurer {
    private final StorageProperties properties;

    public LocalStorageWebConfiguration(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getLocal().getDirectory())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}

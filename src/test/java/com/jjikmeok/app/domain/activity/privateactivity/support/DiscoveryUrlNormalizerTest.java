package com.jjikmeok.app.domain.activity.privateactivity.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryUrlNormalizerTest {

    private final DiscoveryUrlNormalizer discoveryUrlNormalizer = new DiscoveryUrlNormalizer();

    @Test
    void normalize_removesDefaultHttpsPort() {
        String normalized = discoveryUrlNormalizer.normalize("https://Example.com:443/path?utm_source=test&b=2&a=1");

        assertThat(normalized).isEqualTo("https://example.com/path?a=1&b=2");
    }

    @Test
    void normalize_preservesNonDefaultPort() {
        String normalized = discoveryUrlNormalizer.normalize("http://Example.com:8080/path?b=2&a=1");

        assertThat(normalized).isEqualTo("http://example.com:8080/path?a=1&b=2");
    }
}

package com.jjikmeok.app.domain.discovery.support;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DiscoveryUrlNormalizer {

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Set.of(
            "fbclid", "gclid", "igshid", "igsh", "mc_cid", "mc_eid", "si"
    ));

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(value.trim());
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return value.trim();
            }

            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String path = uri.getRawPath();
            String query = normalizeQuery(uri.getRawQuery());

            URI normalized = new URI(
                    scheme,
                    null,
                    host,
                    port,
                    path == null || path.isBlank() ? null : path,
                    query,
                    null
            );
            return normalized.toString();
        } catch (Exception e) {
            return value.trim();
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = Arrays.stream(query.split("&"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .filter(part -> {
                    String key = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
                    return !key.startsWith("utm_") && !TRACKING_PARAMS.contains(key);
                })
                .sorted()
                .collect(Collectors.joining("&"));

        return normalized.isBlank() ? null : normalized;
    }
}

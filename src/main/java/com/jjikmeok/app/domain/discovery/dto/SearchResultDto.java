package com.jjikmeok.app.domain.discovery.dto;

import com.jjikmeok.app.domain.discovery.enums.DiscoverySourceChannel;

public record SearchResultDto(
        String keyword,
        String title,
        String url,
        String snippet,
        Integer position,
        String provider,
        DiscoverySourceChannel sourceChannel
) {
    public SearchResultDto withSourceChannel(DiscoverySourceChannel sourceChannel) {
        return new SearchResultDto(keyword, title, url, snippet, position, provider, sourceChannel);
    }
}

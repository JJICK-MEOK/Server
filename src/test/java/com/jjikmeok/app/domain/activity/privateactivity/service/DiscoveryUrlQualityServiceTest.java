package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySourceChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryUrlQualityServiceTest {

    private final DiscoveryUrlQualityService discoveryUrlQualityService = new DiscoveryUrlQualityService();

    @Test
    void evaluate_matchesSubdomainForSupportedSourceChannel() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto("keyword", "title", "https://m.instagram.com/p/example", "snippet", 1, "provider", null)
        );

        assertThat(assessment.sourceChannel()).isEqualTo(DiscoverySourceChannel.INSTAGRAM);
    }

    @Test
    void evaluate_doesNotMatchContainsLikeHost() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto("keyword", "title", "https://notinstagram.com/post", "snippet", 1, "provider", null)
        );

        assertThat(assessment.sourceChannel()).isEqualTo(DiscoverySourceChannel.WEBSITE);
    }
}

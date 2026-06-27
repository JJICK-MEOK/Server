package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySourceChannel;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
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

    @Test
    void evaluate_marksKoreanAdKeywordsAsExcluded() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto("keyword", "광고 배너", "https://example.com/post", "후원 프로모션 안내", 1, "provider", null)
        );

        assertThat(assessment.excluded()).isTrue();
    }

    @Test
    void evaluate_detectsKoreanFullContentHints() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto("keyword", "공공기관 프로그램", "https://www.seoul.go.kr/event", "지자체 모집 안내", 1, "provider", null)
        );

        assertThat(assessment.extractionMode()).isEqualTo(ExtractionMode.FULL_CONTENT);
    }
}

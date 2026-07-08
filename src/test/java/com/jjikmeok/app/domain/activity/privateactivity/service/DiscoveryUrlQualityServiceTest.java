package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySourceChannel;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryUrlQualityServiceTest {

    private final DiscoveryUrlQualityService discoveryUrlQualityService = new DiscoveryUrlQualityService();

    @Test
    void evaluate_excludesInstagramWithoutOfficialSignal() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto("keyword", "title", "https://m.instagram.com/p/example", "snippet", 1, "provider", null)
        );

        assertThat(assessment.sourceChannel()).isEqualTo(DiscoverySourceChannel.INSTAGRAM);
        assertThat(assessment.excluded()).isTrue();
    }

    @Test
    void evaluate_allowsOfficialInstagramProgramPost() {
        DiscoveryUrlQualityService.Assessment assessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto(
                        "keyword",
                        "공식 인스타그램 계정",
                        "https://www.instagram.com/p/example",
                        "공식 프로그램 모집 안내",
                        1,
                        "provider",
                        null
                )
        );

        assertThat(assessment.sourceChannel()).isEqualTo(DiscoverySourceChannel.INSTAGRAM);
        assertThat(assessment.excluded()).isFalse();
        assertThat(assessment.extractionMode()).isEqualTo(ExtractionMode.METADATA_ONLY);
    }

    @Test
    void evaluate_excludesInstagramProfileAndTagPages() {
        DiscoveryUrlQualityService.Assessment profileAssessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto(
                        "keyword",
                        "공식 인스타그램 계정",
                        "https://www.instagram.com/official_brand/",
                        "공식 프로그램 안내",
                        1,
                        "provider",
                        null
                )
        );
        DiscoveryUrlQualityService.Assessment tagAssessment = discoveryUrlQualityService.evaluate(
                new SearchResultDto(
                        "keyword",
                        "공식 인스타그램 계정",
                        "https://www.instagram.com/explore/tags/program/",
                        "공식 프로그램 안내",
                        1,
                        "provider",
                        null
                )
        );

        assertThat(profileAssessment.excluded()).isTrue();
        assertThat(tagAssessment.excluded()).isTrue();
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
                new SearchResultDto("keyword", "광고 스폰서", "https://example.com/post", "체험 후기 안내", 1, "provider", null)
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

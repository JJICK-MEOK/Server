package com.jjikmeok.app.domain.activity.privateactivity.keyword;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryKeywordServiceTest {

    private final DiscoveryKeywordService keywordService = new DiscoveryKeywordService();

    @Test
    void keywordsFor_generatesSiteRestrictedBrandQueries() {
        assertThat(keywordService.keywordsFor(ActivityCategory.SPORTS, 0))
                .isNotEmpty()
                .allMatch(keyword -> keyword.startsWith("site:"))
                .anyMatch(keyword -> keyword.contains("site:bac.blackyak.com"))
                .anyMatch(keyword -> keyword.contains("운동/액티비티"))
                .anyMatch(keyword -> keyword.contains("원데이클래스"));
    }

    @Test
    void brandSources_containsOnlyVerifiedEnabledSources() {
        assertThat(keywordService.brandSources())
                .allMatch(DiscoveryBrandSource::enabled)
                .noneMatch(source -> source.domain().equals("climbkorea.com"))
                .noneMatch(source -> source.domain().equals("boulderfriends.com"))
                .noneMatch(source -> source.domain().equals("ticketlink.co.kr"))
                .noneMatch(source -> source.domain().equals("ticket.melon.com"))
                .noneMatch(source -> source.domain().equals("leeum.org"));

        assertThat(keywordService.keywordsFor(ActivityCategory.SPORTS, 0))
                .noneMatch(keyword -> keyword.contains("site:climbkorea.com"))
                .noneMatch(keyword -> keyword.contains("site:boulderfriends.com"));
    }

    @Test
    void isAllowedSourceUrlForKeyword_requiresSameSiteDomain() {
        String keyword = "site:ticket.yes24.com 문화/예술 원데이클래스 모집 신청";

        assertThat(keywordService.isAllowedSourceUrlForKeyword(keyword, "https://ticket.yes24.com/perf/52132")).isTrue();
        assertThat(keywordService.isAllowedSourceUrlForKeyword(keyword, "https://example.com/products/123")).isFalse();
    }

    @Test
    void keywordsFor_hasSourcesForProvidedBrandPoolCategories() {
        for (ActivityCategory category : ActivityCategory.values()) {
            if (category == ActivityCategory.LANGUAGE) {
                continue;
            }
            assertThat(keywordService.keywordsFor(category, 0))
                    .as("category=%s", category)
                    .isNotEmpty();
        }
        assertThat(keywordService.keywordsFor(ActivityCategory.LANGUAGE, 0)).isEmpty();
    }
}

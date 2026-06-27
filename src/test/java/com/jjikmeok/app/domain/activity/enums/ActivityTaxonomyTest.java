package com.jjikmeok.app.domain.activity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityTaxonomyTest {

    @Test
    void activityTypes_includeAllDefinedValues() {
        assertThat(ActivityType.values()).hasSize(4);
        assertThat(ActivityType.values())
                .extracting(ActivityType::getLabel)
                .containsExactly("프로그램", "원데이", "행사/강연", "동아리");
    }

    @Test
    void activityCategories_includeAllDefinedValues() {
        assertThat(ActivityCategory.values()).hasSize(10);
        assertThat(ActivityCategory.values())
                .extracting(ActivityCategory::getLabel)
                .containsExactly(
                        "운동 / 액티비티",
                        "문화 / 예술",
                        "공예 / 만들기",
                        "요리 / 베이킹",
                        "사진 / 영상",
                        "책 / 글",
                        "여행 / 탐방",
                        "언어 / 해외",
                        "봉사활동",
                        "성장 / 커리어"
                );
    }

    @Test
    void preferenceTags_areRenderedAsHashtags() {
        assertThat(PreferenceTag.values()).isNotEmpty();
        assertThat(PreferenceTag.values())
                .extracting(PreferenceTag::getHashtag)
                .allMatch(hashtag -> hashtag != null && hashtag.startsWith("#"));
    }
}

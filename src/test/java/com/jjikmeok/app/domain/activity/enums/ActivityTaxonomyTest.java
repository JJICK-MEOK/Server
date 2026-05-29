package com.jjikmeok.app.domain.activity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityTaxonomyTest {

    @Test
    void activityTypes_includeAllDefinedValues() {
        assertThat(ActivityType.values()).hasSize(4);
        assertThat(ActivityType.values())
                .extracting(ActivityType::getLabel)
                .containsExactly("프로그램", "원데이", "행사·강연", "동아리");
    }

    @Test
    void activityCategories_includeAllDefinedValues() {
        assertThat(ActivityCategory.values()).hasSize(14);
        assertThat(ActivityCategory.values())
                .extracting(ActivityCategory::getLabel)
                .containsExactly(
                        "운동/액티비티",
                        "문화/공연/축제",
                        "공예/만들기",
                        "댄스/무용",
                        "요리/베이킹",
                        "사진/영상",
                        "음악/악기",
                        "인문학/책/글",
                        "여행/산책/탐방",
                        "해외/언어",
                        "봉사활동",
                        "자기계발/클래스",
                        "커리어/실무",
                        "기타"
                );
    }

    @Test
    void preferenceTags_includeAllDefinedValues() {
        assertThat(PreferenceTag.values()).hasSize(23);
        assertThat(PreferenceTag.values())
                .extracting(PreferenceTag::getHashtag)
                .containsExactly(
                        "#차분",
                        "#활기",
                        "#힐링",
                        "#편안",
                        "#입문",
                        "#가볍게",
                        "#몰입",
                        "#도전",
                        "#무료",
                        "#유료",
                        "#휴식",
                        "#취미",
                        "#배움",
                        "#사교",
                        "#성장",
                        "#경험",
                        "#하루",
                        "#3일",
                        "#일주일",
                        "#한달",
                        "#3개월",
                        "#6개월이상",
                        "#1년이상"
                );
    }
}

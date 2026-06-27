package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryClassifierTest {

    private final CategoryClassifier categoryClassifier = new CategoryClassifier();

    @Test
    void classifyCategory_returnsNullWhenReservationTextIsUnclassified() {
        ActivityCategory category = categoryClassifier.classifyCategory(
                SourceType.SEOUL_RESERVATION,
                "분류 단서가 없는 일반 안내 문구"
        );

        assertThat(category).isNull();
    }

    @Test
    void classifyType_prioritizesProgramKeywordForMultiDayActivity() {
        ActivityType activityType = categoryClassifier.classifyType(
                SourceType.SEOUL_RESERVATION,
                "프로그램 행사 운영 안내",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 3, 18, 0)
        );

        assertThat(activityType).isEqualTo(ActivityType.PROGRAM);
    }
}

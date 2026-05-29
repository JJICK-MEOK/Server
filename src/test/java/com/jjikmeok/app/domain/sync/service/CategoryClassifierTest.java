package com.jjikmeok.app.domain.sync.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryClassifierTest {

    private final CategoryClassifier classifier = new CategoryClassifier();

    @Test
    void classifyCategory_bySourceAndKeywords() {
        assertThat(classifier.classifyCategory(SourceType.VOLUNTEER_1365, "모집")).isEqualTo(ActivityCategory.VOLUNTEER);
        assertThat(classifier.classifyCategory(SourceType.KOPIS, "국악 콘서트")).isEqualTo(ActivityCategory.MUSIC);
        assertThat(classifier.classifyCategory(SourceType.EXHIBITION, "도예 전시")).isEqualTo(ActivityCategory.CRAFT);
        assertThat(classifier.classifyCategory(SourceType.YOUTH_CONTENT, "직업훈련 채용 실무 과정")).isEqualTo(ActivityCategory.CAREER);
    }

    @Test
    void classifyActivityType_byKeywordsAndReservationDate() {
        LocalDateTime date = LocalDateTime.of(2026, 5, 1, 10, 0);

        assertThat(classifier.classifyType(SourceType.URL_MANUAL, "러닝크루 모임")).isEqualTo(ActivityType.CLUB);
        assertThat(classifier.classifyType(SourceType.URL_MANUAL, "원데이 클래스")).isEqualTo(ActivityType.PROGRAM);
        assertThat(classifier.classifyType(SourceType.URL_MANUAL, "원데이 클래스", date, date.plusHours(2))).isEqualTo(ActivityType.ONE_DAY);
        assertThat(classifier.classifyType(SourceType.URL_MANUAL, "공연 행사", date, date.plusDays(2))).isEqualTo(ActivityType.EVENT);
        assertThat(classifier.classifyType(SourceType.SEOUL_RESERVATION, "예약", date, date.plusHours(2))).isEqualTo(ActivityType.ONE_DAY);
    }
}

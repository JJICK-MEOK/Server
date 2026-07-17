package com.jjikmeok.app.domain.activity.privateactivity.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryDuration;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryGroupSize;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryIntensity;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryMood;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryPurpose;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverySheetRowDtoTest {

    @Test
    void sheetHeaders_matchCurrentTwentyNineColumnSheet() {
        assertThat(DiscoverySheetRowDto.sheetHeaders()).containsExactly(
                "번호",
                "수집 일시",
                "발행 일시",
                "검수자",
                "상태",
                "주제 카테고리",
                "활동 분야",
                "활동명",
                "운영처",
                "링크 URL",
                "썸네일 URL",
                "활동 기간 시작",
                "활동 기간 종료",
                "모집 기간 시작",
                "모집 기간 종료",
                "대상",
                "비용",
                "설명",
                "문의처",
                "지역",
                "장소",
                "분위기 태그 1",
                "분위기 태그 2",
                "강도 태그",
                "목적 태그",
                "기간 태그",
                "규모 태그",
                "원본",
                "신뢰도"
        );
    }

    @Test
    void sheetRow_roundTripsUsingCurrentColumnOrder() {
        DiscoverySheetRowDto original = new DiscoverySheetRowDto(
                217,
                "reviewer",
                DiscoverySheetStatus.READY,
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 18),
                "KOPIS",
                "운영처",
                "활동명",
                "https://example.com/activity",
                "https://example.com/thumbnail.jpg",
                ActivityType.EVENT,
                ActivityCategory.CULTURE,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "성인",
                10000,
                "설명",
                "02-1234-5678",
                "운영처",
                "서울",
                "서울시 중구",
                DiscoveryMood.CALM,
                DiscoveryMood.HEALING,
                DiscoveryIntensity.LIGHT,
                DiscoveryPurpose.HOBBY,
                DiscoveryDuration.SHORT_TERM,
                DiscoveryGroupSize.SMALL,
                100d,
                "시트에 저장하지 않는 검색 스니펫"
        );

        List<Object> values = original.toSheetRow();

        assertThat(values).hasSize(29);
        assertThat(values.get(0)).isEqualTo(217);
        assertThat(values.get(1)).isEqualTo("2026-07-17");
        assertThat(values.get(5)).isEqualTo(ActivityCategory.CULTURE.getLabel());
        assertThat(values.get(6)).isEqualTo(ActivityType.EVENT.getLabel());
        assertThat(values.get(7)).isEqualTo("활동명");
        assertThat(values.get(8)).isEqualTo("운영처");
        assertThat(values.get(9)).isEqualTo("https://example.com/activity");
        assertThat(values.get(27)).isEqualTo("KOPIS");
        assertThat(values.get(28)).isEqualTo(100d);

        DiscoverySheetRowDto restored = DiscoverySheetRowDto.fromSheetRow(300, values);

        assertThat(restored.rowNumber()).isEqualTo(217);
        assertThat(restored.createdAt()).isEqualTo(original.createdAt());
        assertThat(restored.publishedAt()).isEqualTo(original.publishedAt());
        assertThat(restored.reviewer()).isEqualTo(original.reviewer());
        assertThat(restored.status()).isEqualTo(original.status());
        assertThat(restored.category()).isEqualTo(original.category());
        assertThat(restored.activityType()).isEqualTo(original.activityType());
        assertThat(restored.title()).isEqualTo(original.title());
        assertThat(restored.sourceName()).isEqualTo(original.sourceName());
        assertThat(restored.sourceUrl()).isEqualTo(original.sourceUrl());
        assertThat(restored.keyword()).isEqualTo("KOPIS");
        assertThat(restored.confidenceScore()).isEqualTo(100d);
        assertThat(restored.searchSnippet()).isNull();
        assertThat(restored.isPublicApiActivity()).isTrue();

        values.set(27, "문화 행사 검색어");
        DiscoverySheetRowDto discovery = DiscoverySheetRowDto.fromSheetRow(300, values);
        assertThat(discovery.isPublicApiActivity()).isFalse();
        assertThat(discovery.toSheetRow().get(27)).isEqualTo("DISCOVERY");
    }

    @Test
    void fromSheetRow_parsesActualKoreanDropdownTagLabels() {
        List<Object> values = new ArrayList<>(Collections.nCopies(29, null));
        values.set(0, 233);
        values.set(21, "감성적");
        values.set(22, "창의적");
        values.set(23, "입문");
        values.set(24, "배움");
        values.set(25, "한달");
        values.set(26, "소규모");

        DiscoverySheetRowDto row = DiscoverySheetRowDto.fromSheetRow(234, values);

        assertThat(row.moodTag1()).isEqualTo(DiscoveryMood.EMOTIONAL);
        assertThat(row.moodTag2()).isEqualTo(DiscoveryMood.CREATIVE);
        assertThat(row.intensity()).isEqualTo(DiscoveryIntensity.BEGINNER);
        assertThat(row.purpose()).isEqualTo(DiscoveryPurpose.LEARNING);
        assertThat(row.duration()).isEqualTo(DiscoveryDuration.ONE_MONTH);
        assertThat(row.groupSize()).isEqualTo(DiscoveryGroupSize.SMALL);
    }
}

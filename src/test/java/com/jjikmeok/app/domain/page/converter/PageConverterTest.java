package com.jjikmeok.app.domain.page.converter;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityTag;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageConverterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 25);

    @Test
    void toCard_returnsTwoRandomHashtagsFromFiveCandidates() {
        Activity activity = activityWithTags();

        assertThat(PageConverter.toCard(activity, false, TODAY).hashtags()).hasSize(2);
    }

    @Test
    void toDetail_returnsThreeRandomHashtagsFromFiveCandidates() {
        Activity activity = activityWithTags();

        assertThat(PageConverter.toDetail(activity, List.<ActivityImage>of(), false, TODAY).hashtags()).hasSize(3);
    }

    @Test
    void toDetail_usesOnlyOneMoodTagFromTwoMoodTags() {
        Activity activity = activityWithTags();

        List<String> hashtags = PageConverter.toDetail(activity, List.<ActivityImage>of(), false, TODAY).hashtags();

        assertThat(hashtags)
                .doesNotContain(PreferenceTag.FREE.getHashtag())
                .doesNotContain(PreferenceTag.PAID.getHashtag());
        assertThat(hashtags.stream()
                .filter(tag -> tag.equals(PreferenceTag.CALM.getHashtag()) || tag.equals(PreferenceTag.HEALING.getHashtag()))
                .count()).isLessThanOrEqualTo(1);
    }

    private Activity activityWithTags() {
        Region region = Region.builder()
                .name("서울")
                .depth(RegionDepth.PROVINCE)
                .build();
        ReflectionTestUtils.setField(region, "id", 10L);

        Activity activity = Activity.builder()
                .region(region)
                .title("테스트 활동")
                .description("상세 설명")
                .thumbnailUrl("https://example.com/thumb.png")
                .sourceUrl("https://example.com/apply")
                .address("서울")
                .recruitStartAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .recruitEndAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .startAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .activityType(ActivityType.PROGRAM)
                .category(ActivityCategory.CRAFT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(0)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(activity, "id", 1L);

        for (String name : List.of("편안한", "힐링", "가볍게", "취미", "단기", "소규모", "무료")) {
            activity.getTags().add(ActivityTag.create(activity, Tag.create(name, TagType.PREFERENCE_TAG)));
        }
        return activity;
    }
}

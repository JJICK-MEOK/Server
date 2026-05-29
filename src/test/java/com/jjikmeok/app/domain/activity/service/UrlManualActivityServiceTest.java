package com.jjikmeok.app.domain.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.sync.service.CategoryClassifier;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlManualActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private RegionRepository regionRepository;

    private UrlManualActivityService service;

    @BeforeEach
    void setUp() {
        service = new UrlManualActivityService(
                activityRepository,
                regionRepository,
                new CategoryClassifier(),
                new ObjectMapper()
        );
    }

    @Test
    void preview_extractsOpenGraphFirst() {
        String html = """
                <meta property="og:title" content="힙독클럽 모임">
                <meta property="og:description" content="차분한 북토크">
                <meta property="og:image" content="https://example.com/og.png">
                <script type="application/ld+json">
                {"@type":"Event","name":"다른 제목","startDate":"2026-05-01"}
                </script>
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://example.com/event", html);

        assertThat(preview.title()).isEqualTo("힙독클럽 모임");
        assertThat(preview.description()).isEqualTo("차분한 북토크");
        assertThat(preview.thumbnailUrl()).isEqualTo("https://example.com/og.png");
        assertThat(preview.suggestedActivityType()).isEqualTo(ActivityType.CLUB);
    }

    @Test
    void preview_extractsJsonLd() {
        String html = """
                <script type="application/ld+json">
                {"@type":"Event","name":"원데이 클래스","description":"입문 체험",
                 "startDate":"2026-05-01","endDate":"2026-05-01",
                 "image":"https://example.com/json.png",
                 "location":{"name":"성수 라운지"},"offers":{"price":"10000"}}
                </script>
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://example.com/event", html);

        assertThat(preview.title()).isEqualTo("원데이 클래스");
        assertThat(preview.thumbnailUrl()).isEqualTo("https://example.com/json.png");
        assertThat(preview.address()).isEqualTo("성수 라운지");
        assertThat(preview.price()).isEqualTo(10000);
        assertThat(preview.startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(preview.suggestedActivityType()).isEqualTo(ActivityType.ONE_DAY);
    }

    @Test
    void preview_extractsHtmlFallbackAndTags() {
        String html = """
                제목: 러닝크루 하루 체험
                설명: 초보도 가능한 활기 있는 모임
                장소: 한강공원
                가격: 무료
                문의: hello@example.com
                2026-05-01
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://example.com/event", html);

        assertThat(preview.title()).isEqualTo("러닝크루 하루 체험");
        assertThat(preview.address()).isEqualTo("한강공원");
        assertThat(preview.price()).isZero();
        assertThat(preview.thumbnailUrl()).isNull();
        assertThat(preview.suggestedPreferenceTags()).contains(PreferenceTag.FREE, PreferenceTag.LIVELY, PreferenceTag.BEGINNER, PreferenceTag.SOCIAL, PreferenceTag.ONE_DAY);
    }

    @Test
    void preview_prefersContentHeadingOverPlaceholderMetadataAndParsesTicketPrice() {
        String html = """
                <html>
                <head><meta property="og:title" content="t e s t"></head>
                <body>
                <h1>가나다락-글놀이 말놀이</h1>
                <p>기간 2026.05.13.(수) ~ 2026.08.30.(일)</p>
                <p>가격 성인 10,000원 / 청소년·어린이 7,000원 / 48개월 미만 유아 무료</p>
                </body>
                </html>
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://www.hangeul.go.kr/exhibition/666", html);

        assertThat(preview.title()).isEqualTo("가나다락-글놀이 말놀이");
        assertThat(preview.price()).isEqualTo(10000);
    }

    @Test
    void preview_filtersInvalidAddressOrganizerAndRepairsMojibake() {
        String html = """
                <html>
                <body>
                <h1>ììëìê´ 5ì ììë´ì¬ì ëª¨ì§ ìë´</h1>
                <p>장소: 통합예약</p>
                <p>운영기관: 시간 : 10:00~17:00</p>
                </body>
                </html>
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://example.com/event", html);

        assertThat(preview.title()).isEqualTo("아양도서관 5월 자원봉사자 모집 안내");
        assertThat(preview.address()).isNull();
        assertThat(preview.organizer()).isNull();
    }

    @Test
    void preview_allowsPartialMetadata() {
        UrlManualActivityService.Preview preview = service.previewFromHtml(
                "https://instagram.com/p/test",
                "<meta property=\"og:title\" content=\"팝업\">"
        );

        assertThat(preview.title()).isEqualTo("팝업");
        assertThat(preview.description()).isEqualTo("상세 설명은 원문에서 확인하세요.");
        assertThat(preview.thumbnailUrl()).isNull();
        assertThat(preview.address()).isNull();
        assertThat(preview.price()).isNull();
    }

    @Test
    void saveManual_adminCompletesPreviewAndApprovesPublishedActivity() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityRepository.findDuplicate(eq(SourceType.URL_MANUAL), anyString(), eq("https://example.com/event"),
                eq("관리자 보완 제목"), eq(startAt), eq("서울 성수"))).thenReturn(Optional.empty());
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            setId(activity, 10L);
            return activity;
        });

        var saved = service.saveManual(new UrlManualActivityService.ManualCommand(
                1L,
                "관리자 보완 제목",
                null,
                "https://example.com/thumb.png",
                "https://example.com/event",
                "서울 성수",
                "운영팀",
                "hello@example.com",
                "성인",
                startAt,
                endAt,
                null,
                null,
                0,
                ActivityCategory.CRAFT,
                ActivityType.ONE_DAY
        ));

        assertThat(saved.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.description()).isEqualTo("상세 설명은 원문에서 확인하세요.");

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        Activity activity = activityCaptor.getValue();
        when(activityRepository.findByIdWithRegion(10L)).thenReturn(Optional.of(activity));

        var approved = service.approve(10L);

        assertThat(approved.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approved.isActive()).isTrue();
    }

    @Test
    void saveManual_preventsDuplicateByNormalizedUrl() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        Activity existing = Activity.builder()
                .region(region)
                .title("기존 제목")
                .description("기존 설명")
                .sourceUrl("https://example.com/event?foo=1")
                .address("서울 성수")
                .recruitEndAt(LocalDateTime.of(2026, 6, 1, 12, 0))
                .price(10000)
                .activityType(ActivityType.EVENT)
                .category(ActivityCategory.ETC)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build();
        setId(existing, 11L);

        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityRepository.findDuplicate(eq(SourceType.URL_MANUAL), anyString(), eq("https://example.com/event?foo=1"),
                eq("수정 제목"), eq(null), eq("서울 성수"))).thenReturn(Optional.of(existing));

        var response = service.saveManual(new UrlManualActivityService.ManualCommand(
                1L,
                "수정 제목",
                "수정 설명",
                null,
                "https://EXAMPLE.com/event/?utm_source=instagram&foo=1",
                "서울 성수",
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 1, 12, 0),
                20000,
                ActivityCategory.CULTURE,
                ActivityType.EVENT
        ));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.sourceUrl()).isEqualTo("https://example.com/event?foo=1");
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void preview_extractsInstagramOpenGraphUrl() {
        String html = """
                <meta property="og:title" content="Instagram의 찍먹클럽: 원데이 클래스 모집">
                <meta property="og:description" content="차분한 도자기 클래스, 성수에서 만나요.">
                <meta property="og:image" content="https://instagram.cdn/image.jpg">
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml(
                "https://www.instagram.com/p/abc/?igsh=abc&utm_source=ig_web_copy_link",
                html
        );

        assertThat(preview.title()).contains("원데이 클래스 모집");
        assertThat(preview.description()).contains("도자기 클래스");
        assertThat(preview.thumbnailUrl()).isEqualTo("https://instagram.cdn/image.jpg");
        assertThat(preview.sourceUrl()).isEqualTo("https://www.instagram.com/p/abc");
    }

    @Test
    void preview_extractsGeneralHomepageFallback() {
        String html = """
                <html>
                <head><title>서울 도자기 원데이 클래스</title></head>
                <body>
                <p>설명: 초보도 가능한 차분한 만들기 수업</p>
                <p>장소: 서울 성수 공방</p>
                <p>주최: 성수문화센터</p>
                <p>문의: help@example.com</p>
                <p>일시: 2026. 5. 1. 14:00</p>
                <p>참가비: 무료</p>
                </body>
                </html>
                """;

        UrlManualActivityService.Preview preview = service.previewFromHtml("https://example.com/classes/pottery", html);

        assertThat(preview.title()).isEqualTo("서울 도자기 원데이 클래스");
        assertThat(preview.description()).isEqualTo("초보도 가능한 차분한 만들기 수업");
        assertThat(preview.address()).isEqualTo("서울 성수 공방");
        assertThat(preview.organizer()).isEqualTo("성수문화센터");
        assertThat(preview.contactInfo()).isEqualTo("help@example.com");
        assertThat(preview.startAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 14, 0));
        assertThat(preview.price()).isZero();
    }

    private Region region(Long id, String name, RegionDepth depth, Region parent) {
        Region region = Region.builder()
                .parent(parent)
                .name(name)
                .depth(depth)
                .build();
        setId(region, id);
        return region;
    }

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}

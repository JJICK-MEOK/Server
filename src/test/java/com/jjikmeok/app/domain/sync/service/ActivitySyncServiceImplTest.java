package com.jjikmeok.app.domain.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.ai.dto.ExtractedActivityDto;
import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import com.jjikmeok.app.domain.sync.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.sync.dto.NormalizedActivity;
import com.jjikmeok.app.domain.sync.entity.RawActivity;
import com.jjikmeok.app.domain.sync.repository.RawActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivitySyncServiceImplTest {

    @Mock
    private ActivityRegionResolver activityRegionResolver;
    @Mock
    private ExternalActivityGateway externalActivityGateway;
    @Mock
    private ActivityNormalizer activityNormalizer;
    @Mock
    private RawActivityRepository rawActivityRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private ActivityAttachmentStorageService activityAttachmentStorageService;
    @Mock
    private ActivityDetailEnricher activityDetailEnricher;

    // 신규 도메인 및 벡터스토어 의존성 Mock 선언
    @Mock
    private AiActivityParser aiActivityParser;
    @Mock
    private VectorStore vectorStore;

    private ActivitySyncUtils activitySyncUtils;
    private ActivitySyncServiceImpl service;

    @BeforeEach
    void setUp() {
        activitySyncUtils = new ActivitySyncUtils();

        // 11개의 파라미터 규격 생성자 매핑 완료
        service = new ActivitySyncServiceImpl(
                activityRegionResolver,
                externalActivityGateway,
                activityNormalizer,
                rawActivityRepository,
                activityRepository,
                regionRepository,
                new ObjectMapper(),
                activityAttachmentStorageService,
                activityDetailEnricher,
                activitySyncUtils,
                aiActivityParser,
                vectorStore
        );

        // 🌟 잘려 있던 Reflection 주입부를 setUp 블록 내부로 완전 격리 조정
        ReflectionTestUtils.setField(service, "defaultRegionId", 1L);
        ReflectionTestUtils.setField(service, "defaultMaxPages", 1);
        ReflectionTestUtils.setField(service, "monthsAhead", 1);
        ReflectionTestUtils.setField(service, "tourApiBaseUrl", "https://tour");
        ReflectionTestUtils.setField(service, "tourApiServiceKey", "");
        ReflectionTestUtils.setField(service, "tourApiMaxPages", 1);
        ReflectionTestUtils.setField(service, "serverBaseUrl", "http://localhost:8080");

        Region defaultRegion = Region.builder().name("default").depth(RegionDepth.PROVINCE).build();
        when(activityRegionResolver.resolve(any(), any(), any())).thenReturn(defaultRegion);
    }

    @Test
    void sync_updatesDuplicateActivity() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        Activity existing = Activity.builder()
                .region(region)
                .title("기존")
                .description("기존 설명")
                .sourceUrl("https://old")
                .address("예술극장")
                .recruitStartAt(startAt.minusDays(2))
                .recruitEndAt(startAt)
                .activityType(ActivityType.EVENT)
                .category(ActivityCategory.CULTURE)
                .sourceType(SourceType.TOUR_API)
                .externalId("k1")
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        // 1순위 필수 조건 충족 데이터 구성 (모집 시작일 패딩)
        NormalizedActivity normalized = new NormalizedActivity(
                "새 공연", "새 설명", "poster", "https://old", "예술극장",
                null, null, null,
                startAt, startAt.plusDays(1), startAt.minusDays(2), startAt.minusDays(1),
                0, ActivityType.EVENT, ActivityCategory.CULTURE, SourceType.TOUR_API,
                "k1", ApprovalStatus.APPROVED, true
        );

        when(externalActivityGateway.fetchPage(eq(SourceType.TOUR_API), eq("https://tour"), eq(""), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", "{}")).thenReturn(List.of(normalized));
        doReturn(Optional.of(existing))
                .when(activityRepository)
                .findDuplicate(eq(SourceType.TOUR_API), eq("k1"), eq("https://old"), any(), eq(startAt), any());

        ActivitySyncResponse response = service.sync(SourceType.TOUR_API, null);

        assertThat(response.activitySavedCount()).isZero();
        assertThat(response.duplicatedCount()).isEqualTo(1);
        assertThat(existing.getTitle()).isEqualTo("새 공연");
        assertThat(existing.getThumbnailUrl()).isEqualTo("poster");
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void sync_skipsDuplicateWhenSame() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime endAt = startAt.plusDays(1);
        LocalDateTime recruitStartAt = startAt.minusDays(2);
        LocalDateTime recruitEndAt = startAt.minusDays(1);

        Activity existing = spy(Activity.builder()
                .region(region)
                .title("공연")
                .description("설명")
                .thumbnailUrl("poster")
                .sourceUrl("https://old")
                .address("예술극장")
                .startAt(startAt)
                .endAt(endAt)
                .recruitStartAt(recruitStartAt)
                .recruitEndAt(recruitEndAt)
                .price(0)
                .activityType(ActivityType.EVENT)
                .category(ActivityCategory.CULTURE)
                .sourceType(SourceType.TOUR_API)
                .externalId("k1")
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build());

        NormalizedActivity normalized = new NormalizedActivity(
                "공연", "설명", "poster", "https://old", "예술극장",
                null, null, null, startAt, endAt, recruitStartAt, recruitEndAt, 0,
                ActivityType.EVENT, ActivityCategory.CULTURE, SourceType.TOUR_API, "k1", ApprovalStatus.APPROVED, true
        );

        when(externalActivityGateway.fetchPage(eq(SourceType.TOUR_API), eq("https://tour"), eq(""), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", "{}")).thenReturn(List.of(normalized));
        doReturn(Optional.of(existing))
                .when(activityRepository)
                .findDuplicate(eq(SourceType.TOUR_API), eq("k1"), eq("https://old"), any(), eq(startAt), any());

        ActivitySyncResponse response = service.sync(SourceType.TOUR_API, null);

        assertThat(response.duplicatedCount()).isEqualTo(1);
        verify(existing, never()).updateExtra(any(), any(), any());
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void sync_savesLongValuesForTextColumns() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        String title = "가".repeat(150);
        String thumbnailUrl = "https://example.com/" + "t".repeat(600);
        String sourceUrl = "https://example.com/" + "s".repeat(600);
        String address = "주소".repeat(150);
        String organizer = "기관".repeat(80);
        String contactInfo = "연락처".repeat(100);
        String target = "대상".repeat(150);
        String externalId = "external".repeat(30);

        NormalizedActivity normalized = new NormalizedActivity(
                title, "설명", thumbnailUrl, sourceUrl, address, organizer, contactInfo, target,
                startAt, startAt.plusDays(1), startAt.minusDays(2), startAt.minusDays(1),
                0, ActivityType.PROGRAM, ActivityCategory.CULTURE, SourceType.TOUR_API,
                externalId, ApprovalStatus.APPROVED, true
        );

        when(externalActivityGateway.fetchPage(eq(SourceType.TOUR_API), eq("https://tour"), eq(""), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", "{}")).thenReturn(List.of(normalized));
        when(activityRepository.findDuplicate(eq(SourceType.TOUR_API), any(), any(), any(), eq(startAt), any())).thenReturn(Optional.empty());

        service.sync(SourceType.TOUR_API, null, 1);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo(title);
    }

    // 🌟 [비즈니스 정렬 동기화 반영] 2순위 데이터 부재 시 기본 문장값(Default Text) 패딩 메커니즘 정상 작동 검증
    @Test
    void sync_fillsSecondPriorityFieldsWithDefaultsWhenMissing() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);

        NormalizedActivity normalized = new NormalizedActivity(
                "정상 메타 활동", null, "https://thumb.png", "https://example.com/core", "체육관",
                null, null, null, // 2순위 정보 전무
                startAt, startAt.plusDays(2), startAt.minusDays(2), startAt.minusDays(1),
                10000, ActivityType.PROGRAM, ActivityCategory.SPORTS, SourceType.TOUR_API,
                "core-id", ApprovalStatus.APPROVED, true
        );

        when(externalActivityGateway.fetchPage(eq(SourceType.TOUR_API), eq("https://tour"), eq(""), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", "{}")).thenReturn(List.of(normalized));
        when(activityRepository.findDuplicate(eq(SourceType.TOUR_API), eq("core-id"), any(), any(), eq(startAt), any()))
                .thenReturn(Optional.empty());

        service.sync(SourceType.TOUR_API, null, 1);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();

        // 2순위 필드가 누락되었을 때 하네스가 기본 문장값으로 수혈했는지 검증
        assertThat(saved.getDescription()).isEqualTo("상세 설명은 원문에서 확인하세요.");
        assertThat(saved.getOrganizer()).isEqualTo("주최기관 정보는 원문 링크를 확인하세요.");
        assertThat(saved.getContactInfo()).isEqualTo("문의 안내는 원문 링크를 확인하세요.");
        assertThat(saved.getTarget()).isEqualTo("참여 대상은 원문 링크를 확인하세요.");
        assertThat(saved.getIsActive()).isTrue(); // 1순위 일정이 확보되었으므로 활성화 성공
    }

    // 🌟 [신규 검증 아티팩트] 1순위 날짜 결손 감지 시 AI 보완 레이어 파서 연동 호출 확인 테스트
    @Test
    void sync_triggersAiFallbackParserWhen1stPriorityFieldsAreDeficient() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 1, 10, 0);

        // 가격 및 일정 데이터가 꼬여서 유실된 불량 객체 인입 시뮬레이션
        NormalizedActivity deficientNormalized = new NormalizedActivity(
                "결손된 데이터 항목", "본문내용", "https://thumb.png", "https://example.com/err", "장소",
                null, null, null,
                null, null, null, null, null, // 1순위 일정 및 금액 유실됨
                ActivityType.EVENT, ActivityCategory.ETC, SourceType.TOUR_API,
                "def-id", ApprovalStatus.APPROVED, true
        );

        ExtractedActivityDto mockedAiResult = new ExtractedActivityDto(
                startAt.minusDays(2), startAt.minusDays(1), startAt, startAt.plusDays(3), 5000,
                "AI가 복원한 설명", "AI가 복원한 대상", "AI 연락처", "AI 주최사"
        );

        when(externalActivityGateway.fetchPage(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(any(), any(), any(), any())).thenReturn(List.of(deficientNormalized));

        // 하네스 가드 발동에 따른 AI Mock 추론 바인딩
        when(aiActivityParser.parseFallback(any(), any(SourceType.class))).thenReturn(mockedAiResult);

        service.sync(SourceType.TOUR_API, null, 1);

        // 하네스가 차단하지 않고 AI 데이터를 흡수해서 완벽하게 적재했는지 추적
        verify(aiActivityParser).parseFallback(any(), any(SourceType.class));
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void sync_savesAlwaysOpenDatesWhenDatesAreStillMissingAfterScraping() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        NormalizedActivity normalized = new NormalizedActivity(
                "상시 활동", "본문내용", "https://thumb.png", "https://example.com/always", "장소",
                "기관", "010-1234-5678", "성인", null, null, null, null, null,
                ActivityType.EVENT, ActivityCategory.ETC, SourceType.TOUR_API,
                "always-id", ApprovalStatus.APPROVED, true
        );

        when(externalActivityGateway.fetchPage(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(new ExternalActivityGateway.FetchedPayload(SourceType.TOUR_API, "https://tour", "JSON", "{}"));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityNormalizer.normalize(any(), any(), any(), any())).thenReturn(List.of(normalized));
        when(aiActivityParser.parseFallback(any(), any(SourceType.class))).thenReturn(null);
        when(activityRepository.findDuplicate(eq(SourceType.TOUR_API), eq("always-id"), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service.sync(SourceType.TOUR_API, null, 1);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertThat(captor.getValue().getRecruitEndAt().getYear()).isEqualTo(2999);
        assertThat(captor.getValue().getStartAt().getYear()).isEqualTo(2999);
    }

    @Test
    void sync_skipsExternalCallsWhenMaxPagesIsZero() {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));

        ActivitySyncResponse response = service.sync(SourceType.TOUR_API, null, 0);

        assertThat(response.rawSavedCount()).isZero();
        assertThat(response.activitySavedCount()).isZero();
        verify(externalActivityGateway, never()).fetchPage(any(), any(), any(), any(), any(), anyInt());
        verify(rawActivityRepository, never()).save(any());
        verify(activityRepository, never()).save(any());
    }
}

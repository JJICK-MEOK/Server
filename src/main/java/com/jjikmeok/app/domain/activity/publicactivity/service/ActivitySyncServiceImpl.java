package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.ai.dto.ExtractedActivityDto;
import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import com.jjikmeok.app.domain.activity.service.ActivityTagAutoAttachService;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivitySyncServiceImpl implements ActivitySyncService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int HARD_MAX_PAGES = 30;
    private static final int HARD_MAX_MONTHS_AHEAD = 12;
    private static final LocalDateTime ALWAYS_OPEN_AT = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private final ActivityRegionResolver activityRegionResolver;
    private final ExternalActivityGateway externalActivityGateway;
    private final ActivityNormalizer activityNormalizer;
    private final RawActivityArchiveService rawActivityArchiveService;
    private final ActivityRepository activityRepository;
    private final RegionRepository regionRepository;
    private final GoogleSheetsService googleSheetsService;
    private final ActivityAttachmentStorageService activityAttachmentStorageService;
    private final ActivityDetailEnricher activityDetailEnricher;
    private final ActivityTagAutoAttachService activityTagAutoAttachService;
    private final ActivitySyncUtils utils;
    private final AiActivityParser aiActivityParser;
    private final VectorStore vectorStore;

    @Value("${app.activity-sync.default-region-id:1}") private Long defaultRegionId;
    @Value("${app.activity-sync.default-max-pages:1}") private Integer defaultMaxPages;
    @Value("${app.activity-sync.months-ahead:1}") private Integer monthsAhead;
    @Value("${app.activity-sync.kopis.base-url:}") private String kopisBaseUrl;
    @Value("${app.activity-sync.kopis.service-key:}") private String kopisServiceKey;
    @Value("${app.activity-sync.kopis.max-pages:1}") private Integer kopisMaxPages;
    @Value("${app.activity-sync.exhibition.base-url:}") private String exhibitionBaseUrl;
    @Value("${app.activity-sync.exhibition.service-key:}") private String exhibitionServiceKey;
    @Value("${app.activity-sync.exhibition.max-pages:1}") private Integer exhibitionMaxPages;
    @Value("${app.activity-sync.seoul-culture.base-url:}") private String seoulCultureBaseUrl;
    @Value("${app.activity-sync.seoul-culture.max-pages:1}") private Integer seoulCultureMaxPages;
    @Value("${app.activity-sync.seoul-reservation.base-url:}") private String seoulReservationBaseUrl;
    @Value("${app.activity-sync.seoul-reservation.max-pages:1}") private Integer seoulReservationMaxPages;
    @Value("${app.base-url:http://localhost:8080}") private String serverBaseUrl;

    @Override
    @Transactional
    public void syncAllSources() {
        log.info("[ActivitySync] 7개 소스 일괄 동기화 시작");
        for (SourceType source : SourceType.values()) {
            if (!source.isPublicApiSource()) {
                continue;
            }
            try {
                sync(source, null, null);
            } catch (Exception e) {
                log.error("[ActivitySync] {} 동기화 실패: {}", source, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public ActivitySyncResponse sync(SourceType sourceType, ActivityCategory categoryOverride) {
        return sync(sourceType, categoryOverride, null);
    }

    @Override
    @Transactional
    public ActivitySyncResponse sync(SourceType sourceType, ActivityCategory categoryOverride, Integer maxPagesOverride) {
        if (sourceType == null) {
            throw new CustomException(ErrorCode.ACTIVITY_SYNC_UNSUPPORTED_SOURCE);
        }
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate endDate = today.plusMonths(Math.max(0, Math.min(monthsAhead == null ? 1 : monthsAhead, HARD_MAX_MONTHS_AHEAD)));

        activityRepository.deactivateEnded(LocalDateTime.now(SEOUL));

        int rawSaved = 0, saved = 0, duplicated = 0, totalCount = 0, aiFallbackCount = 0;
        Map<String, String> thumbnailCache = new HashMap<>();

        for (String prfstate : sourceType == SourceType.KOPIS ? List.of("02", "01") : List.of("")) {
            for (DateWindow window : windows(sourceType, today, endDate)) {
                int maxPages = Math.max(0, Math.min(
                        maxPagesOverride != null ? maxPagesOverride : configuredMaxPages(sourceType),
                        HARD_MAX_PAGES));

                for (int page = 1; page <= maxPages; page++) {
                    ExternalActivityGateway.FetchedPayload fetchedPayload;
                    try {
                        fetchedPayload = sourceType == SourceType.KOPIS
                                ? externalActivityGateway.fetchKopisPage(baseUrl(sourceType), serviceKey(sourceType), window.start(), window.end(), page, prfstate)
                                : externalActivityGateway.fetchPage(sourceType, baseUrl(sourceType), serviceKey(sourceType), window.start(), window.end(), page);
                    } catch (CustomException e) {
                        log.warn("[ActivitySync] {} API 호출 실패, 동기화 종료. rawSaved={}, saved={}, reason={}",
                                sourceType, rawSaved, saved, e.getErrorCode().getCode());
                        return new ActivitySyncResponse(sourceType, rawSaved, saved, duplicated);
                    }

                    rawActivityArchiveService.archiveFetchedPayload(fetchedPayload);
                    rawSaved++;

                    int pageSize = DEFAULT_PAGE_SIZE;
                    List<NormalizedActivity> normalizedActivities = activityNormalizer.normalize(
                            sourceType, fetchedPayload.requestUrl(), fetchedPayload.contentType(), fetchedPayload.payload());

                    for (NormalizedActivity na : normalizedActivities) {
                        totalCount++;

                        na = enrichThumbnail(sourceType, na, thumbnailCache);
                        if (shouldEnrichDetail(sourceType)) {
                            NormalizedActivity enriched = activityDetailEnricher.enrichIfMissing(na);
                            if (enriched != null) na = enriched;
                        }

                        // AI 보완 처리
                        if (requiresAiFallback(sourceType, na)) {
                            aiFallbackCount++;
                            ExtractedActivityDto aiResult = aiActivityParser.parseFallback(aiContext(sourceType, na), sourceType);
                            if (aiResult != null) {
                                na = na.copyWithFallbackFields(
                                        safeAiText(aiResult.title(), na.title()),
                                        safeAiText(aiResult.address(), na.address()),
                                        safeDate(aiResult.recruitStartAt()),
                                        safeDate(aiResult.recruitEndAt()),
                                        safeDate(aiResult.startAt()),
                                        safeDate(aiResult.endAt()),
                                        aiResult.price(),
                                        safeAiText(aiResult.description(), na.description()),
                                        safeAiText(aiResult.target(), na.target()),
                                        utils.firstText(utils.contactOnly(aiResult.contactInfo()), na.contactInfo()),
                                        safeAiText(aiResult.organizer(), na.organizer())
                                );
                            }
                        }

                        na = withDefaults(na);
                        if (!validForPersist(na)) continue;
                        googleSheetsService.upsertPublicActivity(na);

                        Activity existing = activityRepository.findDuplicate(
                                na.sourceType(), na.externalId(), na.sourceUrl(),
                                na.title(), na.startAt(), na.address()
                        ).orElse(null);

                        if (existing != null) {
                            Region region = activityRegionResolver.resolve(na.title(), na.address(), defaultRegionId);
                            updateIfChanged(existing, region, na, categoryOverride);
                            duplicated++;
                            continue;
                        }

                        Region resolvedRegion = activityRegionResolver.resolve(na.title(), na.address(), defaultRegionId);
                        Activity savedActivity = activityRepository.save(toActivity(resolvedRegion, na, categoryOverride));
                        activityTagAutoAttachService.refresh(savedActivity);
                        saved++;

                        try {
                            String embeddingText = "카테고리: %s\n활동명: %s\n주최사: %s\n참여비용: %d원\n상세내용: %s".formatted(
                                    savedActivity.getCategory().name(), savedActivity.getTitle(),
                                    savedActivity.getOrganizer(), savedActivity.getPrice(), savedActivity.getDescription());
                            vectorStore.accept(List.of(new Document(embeddingText,
                                    Map.of("activityId", savedActivity.getId(), "sourceType", savedActivity.getSourceType().name()))));
                        } catch (Exception e) {
                            log.warn("[ActivitySync] {} VectorDB 임베딩 실패, 저장은 완료됨", sourceType);
                        }
                    }

                    if (pageSize < DEFAULT_PAGE_SIZE) break;
                }
            }
        }

        log.info("[ActivitySync] {} 완료: 전체 {}건 중 AI 보완 {}건", sourceType, totalCount, aiFallbackCount);
        return new ActivitySyncResponse(sourceType, rawSaved, saved, duplicated);
    }

    private String safeAiText(String aiValue, String originalValue) {
        String cleaned = utils.cleanText(aiValue);
        if (utils.isBlank(cleaned) || utils.isFallbackText(cleaned)) return originalValue;
        return cleaned;
    }

    /**
     * AI 보완이 필요한 항목인지 판별합니다.
     */
    private boolean requiresAiFallback(SourceType source, NormalizedActivity activity) {
        boolean missingCore = activity.title() == null || activity.title().isBlank()
                || activity.sourceUrl() == null || activity.sourceUrl().isBlank()
                || activity.startAt() == null || activity.endAt() == null
                || activity.recruitStartAt() == null || activity.recruitEndAt() == null
                || activity.recruitEndAt().getYear() >= 2099
                || activity.price() == null;

        boolean missingExtra = isMissingText(activity.organizer())
                || isMissingText(activity.description())
                || isMissingText(activity.target())
                || isMissingText(activity.contactInfo())
                || isMissingText(activity.address());

        if (missingCore || missingExtra) return true;

        return false;
    }

    private LocalDateTime safeDate(LocalDateTime date) {
        return (date == null || date.getYear() >= 2099) ? null : date;
    }

    private NormalizedActivity withDefaults(NormalizedActivity activity) {
        if (activity == null) return null;

        String description = isMissingText(activity.description()) ? "상세 설명은 원문에서 확인하세요." : activity.description();
        String organizer   = isMissingText(activity.organizer())    ? "주최기관 정보는 원문 링크를 확인하세요." : activity.organizer();
        String contactInfo = utils.firstText(utils.contactOnly(activity.contactInfo()), "문의 안내는 원문 링크를 확인하세요.");
        String target      = isMissingText(activity.target())       ? "참여 대상은 원문 링크를 확인하세요." : activity.target();
        String thumbnail   = utils.isBlank(activity.thumbnailUrl()) ? serverBaseUrl + "/images/defaults/default-activity.png" : activity.thumbnailUrl();
        int    price       = activity.price() == null ? 0 : Math.max(0, activity.price());

        LocalDateTime startAt      = safeDate(activity.startAt());
        LocalDateTime endAt        = safeDate(activity.endAt());
        LocalDateTime recruitStart = safeDate(activity.recruitStartAt());
        LocalDateTime recruitEnd   = safeDate(activity.recruitEndAt());

        if (startAt == null && endAt == null) {
            startAt = ALWAYS_OPEN_AT;
            endAt   = ALWAYS_OPEN_AT;
        } else if (startAt == null) {
            startAt = endAt;
        } else if (endAt == null || endAt.isBefore(startAt)) {
            endAt = startAt;
        }

        if (recruitStart == null && recruitEnd == null) {
            recruitStart = LocalDate.now(SEOUL).atStartOfDay();
            recruitEnd   = ALWAYS_OPEN_AT;
        } else if (recruitStart == null) {
            recruitStart = LocalDate.now(SEOUL).atStartOfDay();
        } else if (recruitEnd == null || recruitEnd.isBefore(recruitStart)) {
            recruitEnd = ALWAYS_OPEN_AT;
        }

        return new NormalizedActivity(
                utils.first(activity.title(), (activity.sourceType() != null ? activity.sourceType().name() : "External") + " activity"),
                description, thumbnail, activity.sourceUrl(), activity.address(),
                organizer, contactInfo, target,
                startAt, endAt, recruitStart, recruitEnd,
                price, activity.activityType(), activity.category(), activity.sourceType(),
                activity.externalId(), activity.approvalStatus(), activity.active()
        );
    }

    private boolean validForPersist(NormalizedActivity activity) {
        return activity != null
                && !utils.isBlank(activity.title())
                && !utils.isBlank(activity.sourceUrl())
                && activity.startAt() != null
                && activity.endAt() != null
                && activity.recruitStartAt() != null
                && activity.recruitEndAt() != null
                && !activity.endAt().isBefore(activity.startAt())
                && !activity.recruitEndAt().isBefore(activity.recruitStartAt());
    }

    private boolean isMissingText(String value) {
        return value == null || value.isBlank()
                || value.contains("원문 링크") || value.contains("확인하세요")
                || value.contains("원문 참조") || value.contains("상세 설명은");
    }

    private String aiContext(SourceType sourceType, NormalizedActivity activity) {
        return """
                [수집 채널]: %s
                [제목]: %s
                [주최사]: %s
                [문의 후보]: %s
                [본문 후보]:
                %s
                [원문 URL]: %s
                """.formatted(
                sourceType.name(), activity.title(), activity.organizer(),
                activity.contactInfo(), focusedExcerpt(utils.firstText(activity.description(), "")),
                activity.sourceUrl());
    }

    private String focusedExcerpt(String text) {
        if (text == null || text.isBlank()) return "";
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 6000) return compact;

        StringBuilder result = new StringBuilder();
        for (String keyword : List.of("문의", "연락", "전화", "이메일", "모집", "신청", "접수", "기간", "일시", "대상", "장소")) {
            int index = compact.indexOf(keyword);
            if (index < 0) continue;
            int start = Math.max(0, index - 300);
            int end   = Math.min(compact.length(), index + 500);
            if (!result.isEmpty()) result.append("\n...\n");
            result.append(compact, start, end);
            if (result.length() >= 5000) break;
        }
        if (!result.isEmpty()) return result.toString();
        return compact.substring(0, 3000) + "\n...\n" + compact.substring(compact.length() - 2000);
    }

    private NormalizedActivity enrichThumbnail(SourceType sourceType, NormalizedActivity activity, Map<String, String> cache) {
        if (activity.thumbnailUrl() != null && activity.thumbnailUrl().startsWith("data:image/")) {
            return withThumbnail(activity, activityAttachmentStorageService.uploadDataImage(sourceType, activity.externalId(), activity.thumbnailUrl()));
        }
        if (!utils.isBlank(activity.thumbnailUrl())) return activity;
        String defaultThumb = defaultThumbnail(sourceType, activity);
        return utils.isBlank(defaultThumb) ? activity : withThumbnail(activity, defaultThumb);
    }

    private String defaultThumbnail(SourceType sourceType, NormalizedActivity activity) {
        return null;
    }

    private NormalizedActivity withThumbnail(NormalizedActivity a, String url) {
        return new NormalizedActivity(
                a.title(), a.description(), url, a.sourceUrl(), a.address(),
                a.organizer(), a.contactInfo(), a.target(),
                a.startAt(), a.endAt(), a.recruitStartAt(), a.recruitEndAt(),
                a.price(), a.activityType(), a.category(), a.sourceType(),
                a.externalId(), a.approvalStatus(), a.active());
    }

    private boolean shouldEnrichDetail(SourceType sourceType) {
        return sourceType != null;
    }

    private Activity toActivity(Region region, NormalizedActivity na, ActivityCategory categoryOverride) {
        return Activity.builder()
                .region(region)
                .title(na.title()).description(na.description()).thumbnailUrl(na.thumbnailUrl())
                .sourceUrl(na.sourceUrl()).address(na.address())
                .organizer(na.organizer()).contactInfo(na.contactInfo()).target(na.target())
                .startAt(na.startAt()).endAt(na.endAt())
                .recruitStartAt(na.recruitStartAt()).recruitEndAt(na.recruitEndAt())
                .price(na.price()).activityType(na.activityType())
                .category(categoryOverride != null ? categoryOverride : na.category())
                .sourceType(na.sourceType()).externalId(na.externalId())
                .approvalStatus(na.approvalStatus()).isActive(isActive(na))
                .build();
    }

    private void updateIfChanged(Activity activity, Region region, NormalizedActivity na, ActivityCategory categoryOverride) {
        NormalizedActivity merged = mergeForUpdate(activity, na);
        boolean active = isActive(merged);
        ActivityCategory category = categoryOverride != null ? categoryOverride : merged.category();
        if (!changed(activity, merged, active) && Objects.equals(activity.getCategory(), category)) return;
        activity.update(region, merged.title(), merged.description(), merged.thumbnailUrl(), merged.sourceUrl(),
                merged.address(), merged.organizer(), merged.contactInfo(), merged.target(),
                merged.startAt(), merged.endAt(), merged.recruitStartAt(), merged.recruitEndAt(),
                merged.price(), merged.activityType(), category, merged.sourceType(), merged.externalId(),
                merged.approvalStatus(), active);
        activity.updateExtra(merged.organizer(), merged.contactInfo(), merged.target());
        activityTagAutoAttachService.refresh(activity);
    }

    private NormalizedActivity mergeForUpdate(Activity existing, NormalizedActivity incoming) {
        return new NormalizedActivity(
                betterText(existing.getTitle(), incoming.title()),
                betterText(existing.getDescription(), incoming.description()),
                betterMedia(existing.getThumbnailUrl(), incoming.thumbnailUrl()),
                betterUrl(existing.getSourceUrl(), incoming.sourceUrl()),
                betterText(existing.getAddress(), incoming.address()),
                betterText(existing.getOrganizer(), incoming.organizer()),
                betterContact(existing.getContactInfo(), incoming.contactInfo()),
                betterText(existing.getTarget(), incoming.target()),
                betterDate(existing.getStartAt(), incoming.startAt()),
                betterDate(existing.getEndAt(), incoming.endAt()),
                betterRecruitStart(existing.getRecruitStartAt(), incoming.recruitStartAt()),
                betterRecruitEnd(existing.getRecruitEndAt(), incoming.recruitEndAt()),
                incoming.price() != null ? Math.max(0, incoming.price()) : existing.getPrice(),
                incoming.activityType() != null ? incoming.activityType() : existing.getActivityType(),
                incoming.category() != null ? incoming.category() : existing.getCategory(),
                incoming.sourceType() != null ? incoming.sourceType() : existing.getSourceType(),
                betterText(existing.getExternalId(), incoming.externalId()),
                incoming.approvalStatus() != null ? incoming.approvalStatus() : existing.getApprovalStatus(),
                incoming.active()
        );
    }

    private String betterText(String existing, String incoming) {
        return !isMissingText(incoming) ? incoming : existing;
    }

    private String betterContact(String existing, String incoming) {
        String incomingContact = utils.contactOnly(incoming);
        if (!utils.isBlank(incomingContact)) return incomingContact;
        String existingContact = utils.contactOnly(existing);
        return !utils.isBlank(existingContact) ? existingContact : existing;
    }

    private String betterUrl(String existing, String incoming) {
        return !utils.isBlank(incoming) ? incoming : existing;
    }

    private String betterMedia(String existing, String incoming) {
        if (!utils.isBlank(incoming) && !incoming.contains("/images/defaults/")) return incoming;
        return !utils.isBlank(existing) ? existing : incoming;
    }

    private LocalDateTime betterDate(LocalDateTime existing, LocalDateTime incoming) {
        if (incoming != null && incoming.getYear() < 2999) return incoming;
        return existing != null ? existing : incoming;
    }

    private LocalDateTime betterRecruitStart(LocalDateTime existing, LocalDateTime incoming) {
        if (incoming != null && (existing == null || existing.getYear() >= 2999)) return incoming;
        return existing != null ? existing : incoming;
    }

    private LocalDateTime betterRecruitEnd(LocalDateTime existing, LocalDateTime incoming) {
        return betterDate(existing, incoming);
    }

    private boolean changed(Activity a, NormalizedActivity na, boolean active) {
        return !Objects.equals(a.getTitle(), na.title())
                || !Objects.equals(a.getDescription(), na.description())
                || !Objects.equals(a.getThumbnailUrl(), na.thumbnailUrl())
                || !Objects.equals(a.getSourceUrl(), na.sourceUrl())
                || !Objects.equals(a.getAddress(), na.address())
                || !Objects.equals(a.getStartAt(), na.startAt())
                || !Objects.equals(a.getEndAt(), na.endAt())
                || !Objects.equals(a.getRecruitStartAt(), na.recruitStartAt())
                || !Objects.equals(a.getRecruitEndAt(), na.recruitEndAt())
                || !Objects.equals(a.getPrice(), na.price())
                || !Objects.equals(a.getActivityType(), na.activityType())
                || !Objects.equals(a.getCategory(), na.category())
                || !Objects.equals(a.getSourceType(), na.sourceType())
                || !Objects.equals(a.getExternalId(), na.externalId())
                || !Objects.equals(a.getApprovalStatus(), na.approvalStatus())
                || !Objects.equals(a.getOrganizer(), na.organizer())
                || !Objects.equals(a.getContactInfo(), na.contactInfo())
                || !Objects.equals(a.getTarget(), na.target())
                || !Objects.equals(a.getIsActive(), active);
    }

    private boolean isActive(NormalizedActivity na) {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime todayStart = LocalDate.now(SEOUL).atStartOfDay();
        boolean hasSchedule = na.recruitStartAt() != null || na.recruitEndAt() != null
                || na.startAt() != null || na.endAt() != null;
        return (na.active() == null || na.active())
                && na.approvalStatus().isApproved()
                && hasSchedule
                && (na.recruitEndAt() == null || !na.recruitEndAt().isBefore(todayStart))
                && (na.endAt() == null || !na.endAt().isBefore(now));
    }

    private List<DateWindow> windows(SourceType sourceType, LocalDate today, LocalDate endDate) {
        if (sourceType != SourceType.KOPIS) return List.of(new DateWindow(today, endDate));
        List<DateWindow> windows = new ArrayList<>();
        for (LocalDate start = today; !start.isAfter(endDate); start = start.plusDays(32)) {
            LocalDate end = start.plusDays(31);
            windows.add(new DateWindow(start, end.isAfter(endDate) ? endDate : end));
        }
        return windows;
    }

    private Integer configuredMaxPages(SourceType sourceType) {
        return switch (sourceType) {
            case KOPIS             -> kopisMaxPages;
            case EXHIBITION        -> exhibitionMaxPages;
            case SEOUL_CULTURE     -> seoulCultureMaxPages;
            case SEOUL_RESERVATION -> seoulReservationMaxPages;
            default                -> defaultMaxPages;
        };
    }

    private String baseUrl(SourceType src) {
        return switch (src) {
            case KOPIS             -> kopisBaseUrl;
            case EXHIBITION        -> exhibitionBaseUrl;
            case SEOUL_CULTURE     -> seoulCultureBaseUrl;
            case SEOUL_RESERVATION -> seoulReservationBaseUrl;
            default -> throw new CustomException(ErrorCode.ACTIVITY_SYNC_UNSUPPORTED_SOURCE);
        };
    }

    private String serviceKey(SourceType src) {
        return switch (src) {
            case KOPIS          -> kopisServiceKey;
            case EXHIBITION     -> exhibitionServiceKey;
            default             -> "";
        };
    }

    private record DateWindow(LocalDate start, LocalDate end) {}
}

package com.jjikmeok.app.domain.activity.publicactivity.publish;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.domain.region.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicActivityPublishService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GoogleSheetsService googleSheetsService;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final ActivityRegionResolver activityRegionResolver;
    private final ActivitySyncUtils utils;

    @Value("${app.activity-sync.default-region-id:1}")
    private Long defaultRegionId;

    public int publishReadyRows() {
        int publishedCount = 0;
        List<DiscoverySheetRowDto> readyRows = googleSheetsService.findReadyRows();
        for (DiscoverySheetRowDto row : readyRows) {
            if (row == null) {
                continue;
            }
            if (process(row)) {
                publishedCount++;
            }
        }
        return publishedCount;
    }

    private boolean process(DiscoverySheetRowDto row) {
        DiscoverySheetRowDto reviewing = row.withStatus(DiscoverySheetStatus.REVIEWING, null);
        try {
            googleSheetsService.updateRow(reviewing);
        } catch (Exception e) {
            log.warn("[공공발행] 시트 상태를 검토중으로 바꾸지 못했습니다. row={}, reason={}", row.rowNumber(), e.getMessage(), e);
            return false;
        }

        try {
            String duplicateReason = findDuplicateReason(reviewing).orElse(null);
            if (duplicateReason != null) {
                googleSheetsService.updateRow(reviewing.withStatus(DiscoverySheetStatus.DUPLICATE, null));
                log.info("[공공발행] 중복으로 판단되어 발행하지 않았습니다. row={}, reason={}", row.rowNumber(), duplicateReason);
                return false;
            }

            SourceType sourceType = resolveSourceType(reviewing);
            Region region = activityRegionResolver.resolve(reviewing.regionName(), reviewing.title(), reviewing.address(), defaultRegionId);
            ActivityRequest request = toActivityRequest(reviewing, region.getId(), sourceType);
            ActivityDetailResponse response = activityService.createActivity(request);
            if (response == null) {
                throw new IllegalStateException("활동 생성 응답이 비어 있습니다.");
            }

            googleSheetsService.updateRow(reviewing.withStatus(DiscoverySheetStatus.PUBLISHED, LocalDate.now(SEOUL)));
            log.info("[공공발행] 활동 발행 완료. row={}, activityId={}", row.rowNumber(), response.id());
            return true;
        } catch (Exception e) {
            try {
                googleSheetsService.updateRow(reviewing.withStatus(DiscoverySheetStatus.ERROR, null));
            } catch (Exception updateError) {
                log.error("[공공발행] 오류 상태 업데이트에 실패했습니다. row={}, reason={}", row.rowNumber(), updateError.getMessage(), updateError);
            }
            log.warn("[공공발행] 활동 발행 실패. row={}, reason={}", row.rowNumber(), e.getMessage(), e);
            return false;
        }
    }

    private ActivityRequest toActivityRequest(DiscoverySheetRowDto row, Long regionId, SourceType sourceType) {
        String title = firstText(row.title(), row.sourceUrl(), "공공 활동");
        String description = firstText(row.description(), title, "상세 설명은 원문에서 확인하세요.");
        String sourceUrl = firstText(row.sourceUrl());
        String organizer = firstText(row.organizer(), row.sourceName(), title);
        String contactInfo = row.contactInfo();
        String target = row.target();
        String address = row.address();
        Integer price = row.price() == null ? 0 : row.price();
        ActivityCategory category = row.category() == null ? ActivityCategory.CULTURE : row.category();
        ActivityType activityType = row.activityType() == null ? ActivityType.EVENT : row.activityType();
        LocalDateTime startAt = atStartOfDay(row.startAt());
        LocalDateTime endAt = atEndOfDay(row.endAt());
        LocalDateTime recruitStartAt = atStartOfDay(row.recruitStartAt());
        LocalDateTime recruitEndAt = row.recruitEndAt() != null
                ? atEndOfDay(row.recruitEndAt())
                : (row.endAt() != null ? atEndOfDay(row.endAt()) : LocalDateTime.now(SEOUL).plusMonths(1));

        return new ActivityRequest(
                regionId,
                title,
                description,
                row.thumbnailUrl(),
                sourceUrl,
                address,
                organizer,
                contactInfo,
                target,
                startAt,
                endAt,
                recruitStartAt,
                recruitEndAt,
                price,
                activityType,
                category,
                sourceType,
                createExternalId(sourceType, sourceUrl, title),
                ApprovalStatus.APPROVED,
                true
        );
    }

    private java.util.Optional<String> findDuplicateReason(DiscoverySheetRowDto row) {
        SourceType sourceType = resolveSourceType(row);
        String sourceUrl = row.sourceUrl();
        String title = row.title();
        String organizer = row.organizer();
        LocalDateTime startAt = row.startAt() == null ? null : row.startAt().atStartOfDay();

        if (sourceType != null && sourceUrl != null) {
            boolean duplicate = activityRepository.findDuplicate(
                    sourceType,
                    createExternalId(sourceType, sourceUrl, title),
                    sourceUrl,
                    title,
                    startAt,
                    row.address()
            ).isPresent();
            if (duplicate) {
                return java.util.Optional.of("이미 DB에 동일한 활동이 있습니다.");
            }
        }

        return java.util.Optional.empty();
    }

    private SourceType resolveSourceType(DiscoverySheetRowDto row) {
        SourceType keywordSourceType = parseSourceType(row.keyword());
        if (keywordSourceType != null && keywordSourceType.isPublicApiSource()) {
            return keywordSourceType;
        }

        SourceType urlSourceType = inferSourceTypeFromUrl(row.sourceUrl());
        if (urlSourceType != null) {
            return urlSourceType;
        }

        throw new IllegalStateException("공공 활동의 sourceType을 판별할 수 없습니다. row=" + row.rowNumber());
    }

    private SourceType parseSourceType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private SourceType inferSourceTypeFromUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        String normalized = sourceUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("kopis")) {
            return SourceType.KOPIS;
        }
        if (normalized.contains("seoul")) {
            if (normalized.contains("reservation") || normalized.contains("yeyak")) {
                return SourceType.SEOUL_RESERVATION;
            }
            return SourceType.SEOUL_CULTURE;
        }
        if (normalized.contains("exhibition")) {
            return SourceType.EXHIBITION;
        }
        return null;
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String createExternalId(SourceType sourceType, String sourceUrl, String title) {
        return hashExternalIdSeed(sourceType.name() + "|" + sourceUrl + "|" + title);
    }

    private String hashExternalIdSeed(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }
}

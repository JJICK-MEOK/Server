package com.jjikmeok.app.domain.activity.privateactivity.publish;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.deduplication.DiscoveryDeduplicationService;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.domain.activity.service.SheetActivityTagResolver;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryPublishService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GoogleSheetsService googleSheetsService;
    private final DiscoveryDeduplicationService deduplicationService;
    private final ActivityService activityService;
    private final ActivityRegionResolver activityRegionResolver;
    private final ActivitySyncUtils utils;
    private final SheetActivityTagResolver sheetActivityTagResolver;

    @Value("${app.activity-sync.default-region-id:1}")
    private Long defaultRegionId;

    public int publishReadyRows() {
        List<DiscoverySheetRowDto> readyRows = googleSheetsService.findReadyRows().stream()
                .filter(row -> row != null && !row.isPublicApiActivity())
                .toList();
        if (readyRows.isEmpty()) {
            return 0;
        }

        List<DiscoverySheetRowDto> reviewingRows = readyRows.stream()
                .map(row -> row.withStatus(DiscoverySheetStatus.REVIEWING, null))
                .toList();
        try {
            googleSheetsService.updateRows(reviewingRows);
        } catch (Exception e) {
            log.warn("[발행] 시트 상태 일괄 변경에 실패했습니다. rows={}, reason={}",
                    reviewingRows.stream().map(DiscoverySheetRowDto::rowNumber).toList(), e.getMessage(), e);
            return 0;
        }

        int publishedCount = 0;
        List<DiscoverySheetRowDto> completedRows = new ArrayList<>(reviewingRows.size());
        for (DiscoverySheetRowDto reviewing : reviewingRows) {
            PublishResult result = process(reviewing);
            completedRows.add(result.row());
            if (result.published()) {
                publishedCount++;
            }
        }

        try {
            googleSheetsService.updateRows(completedRows);
        } catch (Exception e) {
            log.error("[발행] 최종 시트 상태 일괄 변경에 실패했습니다. rows={}, reason={}",
                    completedRows.stream().map(DiscoverySheetRowDto::rowNumber).toList(), e.getMessage(), e);
        }
        return publishedCount;
    }

    private PublishResult process(DiscoverySheetRowDto reviewing) {
        try {
            String duplicateReason = deduplicationService.findDuplicateReason(
                    reviewing.sourceUrl(), reviewing.title(), reviewing.organizer()).orElse(null);

            if (duplicateReason != null) {
                log.info("[발행] 중복으로 판단되어 발행하지 않았습니다. row={}, reason={}", reviewing.rowNumber(), duplicateReason);
                return new PublishResult(reviewing.withStatus(DiscoverySheetStatus.DUPLICATE, null), false);
            }

            Region region = activityRegionResolver.resolve(reviewing.regionName(), reviewing.title(), reviewing.address(), defaultRegionId);
            ActivityRequest request = toActivityRequest(reviewing, region.getId());
            List<Long> tagIds = sheetActivityTagResolver.resolveTagIds(reviewing);
            ActivityDetailResponse response = activityService.createActivityWithTags(request, tagIds);
            if (response == null) {
                throw new IllegalStateException("활동 생성 응답이 비어 있습니다.");
            }

            log.info("[발행] 활동 발행을 완료했습니다. row={}, activityId={}", reviewing.rowNumber(), response.id());
            return new PublishResult(
                    reviewing.withStatus(DiscoverySheetStatus.PUBLISHED, LocalDate.now(SEOUL)),
                    true
            );
        } catch (Exception e) {
            log.warn("[발행] 활동 발행에 실패했습니다. row={}, reason={}", reviewing.rowNumber(), e.getMessage(), e);
            return new PublishResult(reviewing.withStatus(DiscoverySheetStatus.ERROR, null), false);
        }
    }

    private record PublishResult(DiscoverySheetRowDto row, boolean published) {
    }

    private ActivityRequest toActivityRequest(DiscoverySheetRowDto row, Long regionId) {
        String title = firstText(row.title(), row.keyword(), row.searchSnippet(), row.sourceUrl());
        String description = firstText(row.description(), title, "활동 설명은 원문에서 확인하세요.");
        String sourceUrl = firstText(row.sourceUrl());
        String organizer = row.organizer();
        String contactInfo = row.contactInfo();
        String target = row.target();
        String address = row.address();
        Integer price = row.price() == null ? 0 : row.price();
        ActivityCategory category = requireCategory(row.category());
        ActivityType activityType = requireActivityType(row.activityType());
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
                SourceType.DISCOVERY,
                createDiscoveryExternalId(sourceUrl),
                ApprovalStatus.APPROVED,
                true
        );
    }

    private ActivityCategory requireCategory(ActivityCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("주제 카테고리를 올바르게 입력해야 합니다.");
        }
        return category;
    }

    private ActivityType requireActivityType(ActivityType activityType) {
        if (activityType == null) {
            throw new IllegalArgumentException("활동 분야를 올바르게 입력해야 합니다.");
        }
        return activityType;
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

    private String createDiscoveryExternalId(String sourceUrl) {
        return hashExternalIdSeed(sourceUrl);
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

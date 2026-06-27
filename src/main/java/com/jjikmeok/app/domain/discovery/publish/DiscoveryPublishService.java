package com.jjikmeok.app.domain.discovery.publish;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.domain.discovery.deduplication.DiscoveryDeduplicationService;
import com.jjikmeok.app.domain.discovery.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.discovery.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.discovery.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.sync.service.ActivityRegionResolver;
import com.jjikmeok.app.domain.sync.service.ActivitySyncUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
            log.warn("[Publish] 상태 변경 실패 row={}, reason={}", row.rowNumber(), e.getMessage(), e);
            return false;
        }

        try {
            String duplicateReason = deduplicationService.findDuplicateReason(
                    reviewing.sourceUrl(),
                    reviewing.title(),
                    reviewing.organizer()
            ).orElse(null);

            if (duplicateReason != null) {
                googleSheetsService.updateRow(reviewing.withStatus(DiscoverySheetStatus.DUPLICATE, null));
                log.info("[Publish] 중복 데이터 제외 row={}, reason={}", row.rowNumber(), duplicateReason);
                return true;
            }

            Region region = activityRegionResolver.resolve(reviewing.title(), reviewing.address(), defaultRegionId);
            ActivityRequest request = toActivityRequest(reviewing, region.getId());
            ActivityDetailResponse response = activityService.createActivity(request);
            if (response == null) {
                throw new IllegalStateException("Activity 생성 결과가 비어 있습니다.");
            }

            googleSheetsService.updateRow(reviewing.withStatus(
                    DiscoverySheetStatus.PUBLISHED,
                    LocalDateTime.now(SEOUL)
            ));
            log.info("[Publish] Activity 생성 완료 row={}, activityId={}", row.rowNumber(), response.id());
            return true;
        } catch (Exception e) {
            try {
                googleSheetsService.updateRow(reviewing.withStatus(DiscoverySheetStatus.ERROR, null));
            } catch (Exception updateError) {
                log.error("[Publish] 오류 상태 반영 실패 row={}, reason={}", row.rowNumber(), updateError.getMessage(), updateError);
            }
            log.warn("[Publish] 발행 실패 row={}, reason={}", row.rowNumber(), e.getMessage(), e);
            return false;
        }
    }

    private ActivityRequest toActivityRequest(DiscoverySheetRowDto row, Long regionId) {
        String title = firstText(row.title(), row.keyword(), row.searchSnippet(), row.sourceUrl());
        String description = firstText(row.description(), title, "상세 설명이 없습니다.");
        String sourceUrl = firstText(row.sourceUrl());
        String organizer = row.organizer();
        String contactInfo = row.contactInfo();
        String target = row.target();
        String address = row.address();
        Integer price = row.price() == null ? 0 : row.price();
        ActivityCategory category = row.category() == null ? ActivityCategory.CULTURE : row.category();
        ActivityType activityType = row.activityType() == null ? ActivityType.EVENT : row.activityType();
        LocalDateTime recruitEndAt = row.recruitEndAt() != null ? row.recruitEndAt()
                : (row.endAt() != null ? row.endAt() : LocalDateTime.now(SEOUL).plusMonths(1));

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
                row.startAt(),
                row.endAt(),
                row.recruitStartAt(),
                recruitEndAt,
                price,
                activityType,
                category,
                SourceType.DISCOVERY,
                hash(sourceUrl),
                ApprovalStatus.APPROVED,
                true
        );
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String hash(String value) {
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

package com.jjikmeok.app.domain.ai.dto;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.discovery.enums.DiscoveryDuration;
import com.jjikmeok.app.domain.discovery.enums.DiscoveryGroupSize;
import com.jjikmeok.app.domain.discovery.enums.DiscoveryIntensity;
import com.jjikmeok.app.domain.discovery.enums.DiscoveryMood;
import com.jjikmeok.app.domain.discovery.enums.DiscoveryPurpose;
import com.jjikmeok.app.domain.discovery.enums.ExtractionMode;

import java.time.LocalDateTime;

public record DiscoveryAnalysisDto(
        String keyword,
        String sourceName,
        String title,
        String sourceUrl,
        String thumbnailUrl,
        String description,
        String organizer,
        String contactInfo,
        String target,
        String address,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        Integer price,
        ActivityCategory category,
        ActivityType activityType,
        DiscoveryMood moodTag1,
        DiscoveryMood moodTag2,
        DiscoveryIntensity intensity,
        DiscoveryPurpose purpose,
        DiscoveryDuration duration,
        DiscoveryGroupSize groupSize,
        double confidenceScore,
        String searchSnippet,
        ExtractionMode extractionMode
) {
}

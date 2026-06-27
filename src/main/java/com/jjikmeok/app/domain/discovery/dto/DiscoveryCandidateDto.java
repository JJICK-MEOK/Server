package com.jjikmeok.app.domain.discovery.dto;

import com.jjikmeok.app.domain.discovery.enums.ExtractionMode;

import java.time.LocalDateTime;

public record DiscoveryCandidateDto(
        String keyword,
        SearchResultDto searchResult,
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
        ExtractionMode extractionMode,
        double confidenceScore,
        String pageText
) {
}

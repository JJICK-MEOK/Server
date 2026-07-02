package com.jjikmeok.app.domain.ai.dto;

import java.time.LocalDateTime;

public record ExtractedActivityDto(
        String title,
        String address,
        String category,
        String activityType,
        String moodTag1,
        String moodTag2,
        String intensity,
        String purpose,
        String duration,
        String groupSize,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer price,
        String description,
        String target,
        String contactInfo,
        String organizer
) {
}

package com.jjikmeok.app.domain.ai.dto;

import java.time.LocalDateTime;

public record ExtractedActivityDto(
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer price,
        String description,
        String target,
        String contactInfo,
        String organizer
) {}
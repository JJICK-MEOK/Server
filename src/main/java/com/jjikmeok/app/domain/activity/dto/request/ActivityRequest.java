package com.jjikmeok.app.domain.activity.dto.request;

import com.jjikmeok.app.domain.activity.enums.AgeRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ActivityRequest(
        @NotNull(message = "지역 ID는 필수입니다.") Long regionId,
        @NotBlank(message = "제목은 필수입니다.") String title,
        String thumbnailUrl,
        @NotBlank(message = "URI는 필수입니다.") String uri,
        String location,
        LocalDateTime recruitStartAt,
        @NotNull(message = "모집 마감일은 필수입니다.") LocalDateTime recruitEndAt,
        LocalDateTime activityStartAt,
        LocalDateTime activityEndAt,
        AgeRange ageRange,
        Integer price,
        @NotBlank(message = "상세 설명은 필수입니다.") String description,
        Boolean isActive
) {}
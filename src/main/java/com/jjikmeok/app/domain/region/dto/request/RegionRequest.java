package com.jjikmeok.app.domain.region.dto.request;

import com.jjikmeok.app.domain.region.enums.RegionDepth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegionRequest(
        Long parentId,
        @NotBlank(message = "지역명은 필수입니다.") String name,
        @NotNull(message = "지역 단계(depth)는 필수입니다.") RegionDepth depth
) {}
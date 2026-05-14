package com.jjikmeok.app.domain.region.dto.response;

import com.jjikmeok.app.domain.region.enums.RegionDepth;

public record RegionResponse(
        Long id,
        Long parentId,
        String name,
        RegionDepth depth
) {}
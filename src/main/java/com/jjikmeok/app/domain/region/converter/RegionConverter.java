package com.jjikmeok.app.domain.region.converter;

import com.jjikmeok.app.domain.region.dto.response.RegionResponse;
import com.jjikmeok.app.domain.region.entity.Region;

public class RegionConverter {
    public static RegionResponse toResponse(Region region) {
        Long parentId = (region.getParent() != null) ? region.getParent().getId() : null;
        return new RegionResponse(
                region.getId(),
                parentId,
                region.getName(),
                region.getDepth()
        );
    }
}
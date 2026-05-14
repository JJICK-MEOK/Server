package com.jjikmeok.app.domain.region.service;

import com.jjikmeok.app.domain.region.dto.request.RegionRequest;
import com.jjikmeok.app.domain.region.dto.response.RegionResponse;

import java.util.List;

public interface RegionService {
    List<RegionResponse> getRegions(Long parentId);
    List<RegionResponse> getTopLevelRegions();
    List<RegionResponse> getSubRegions(Long parentId);
    RegionResponse getRegionById(Long id);
    RegionResponse createRegion(RegionRequest request);
    RegionResponse updateRegion(Long id, RegionRequest request);
    void deleteRegion(Long id);
}
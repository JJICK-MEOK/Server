package com.jjikmeok.app.domain.region.service;

import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.converter.RegionConverter;
import com.jjikmeok.app.domain.region.dto.request.RegionRequest;
import com.jjikmeok.app.domain.region.dto.response.RegionResponse;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final ActivityRepository activityRepository;

    @Override
    public List<RegionResponse> getRegions(Long parentId) {
        if (parentId == null) {
            return getTopLevelRegions();
        }
        return getSubRegions(parentId);
    }

    @Override
    public List<RegionResponse> getTopLevelRegions() {
        return regionRepository.findByParentIdIsNull().stream()
                .map(RegionConverter::toResponse)
                .toList();
    }

    @Override
    public List<RegionResponse> getSubRegions(Long parentId) {
        return regionRepository.findByParentId(parentId).stream()
                .map(RegionConverter::toResponse)
                .toList();
    }

    @Override
    public RegionResponse getRegionById(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        return RegionConverter.toResponse(region);
    }

    @Override
    @Transactional
    public RegionResponse createRegion(RegionRequest request) {
        String normalizedName = request.name().trim();
        Region parent = findParent(request.parentId());
        validateHierarchy(request.depth(), parent);
        validateDuplicateNameOnCreate(parent, normalizedName);

        Region region = Region.builder()
                .parent(parent)
                .name(normalizedName)
                .depth(request.depth())
                .build();

        try {
            return RegionConverter.toResponse(regionRepository.save(region));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.REGION_DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public RegionResponse updateRegion(Long id, RegionRequest request) {
        String normalizedName = request.name().trim();
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        if (region.getDepth() == RegionDepth.PROVINCE
                && request.depth() == RegionDepth.DISTRICT
                && regionRepository.existsByParentId(id)) {
            throw new CustomException(ErrorCode.REGION_HAS_CHILDREN);
        }

        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new CustomException(ErrorCode.REGION_SELF_PARENT_NOT_ALLOWED);
        }

        Region parent = findParent(request.parentId());
        validateHierarchy(request.depth(), parent);
        validateDuplicateNameOnUpdate(id, parent, normalizedName);

        region.update(parent, normalizedName, request.depth());
        return RegionConverter.toResponse(region);
    }

    @Override
    @Transactional
    public void deleteRegion(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        if (regionRepository.existsByParentId(id)) {
            throw new CustomException(ErrorCode.REGION_HAS_CHILDREN);
        }

        if (activityRepository.existsByRegionId(id)) {
            throw new CustomException(ErrorCode.REGION_IN_USE);
        }

        try {
            regionRepository.delete(region);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.REGION_IN_USE);
        }
    }

    private Region findParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return regionRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_PARENT_NOT_FOUND));
    }

    private void validateHierarchy(RegionDepth depth, Region parent) {
        if (depth == RegionDepth.PROVINCE && parent != null) {
            throw new CustomException(ErrorCode.REGION_PARENT_NOT_ALLOWED);
        }
        if (depth == RegionDepth.DISTRICT && parent == null) {
            throw new CustomException(ErrorCode.REGION_PARENT_REQUIRED);
        }
        if (depth == RegionDepth.DISTRICT && parent != null && parent.getDepth() != RegionDepth.PROVINCE) {
            throw new CustomException(ErrorCode.REGION_INVALID_PARENT_DEPTH);
        }
    }

    private void validateDuplicateNameOnCreate(Region parent, String name) {
        boolean duplicated = parent == null
                ? regionRepository.existsByParentIsNullAndName(name)
                : regionRepository.existsByParentIdAndName(parent.getId(), name);

        if (duplicated) {
            throw new CustomException(ErrorCode.REGION_DUPLICATE_NAME);
        }
    }

    private void validateDuplicateNameOnUpdate(Long id, Region parent, String name) {
        boolean duplicated = parent == null
                ? regionRepository.existsByParentIsNullAndNameAndIdNot(name, id)
                : regionRepository.existsByParentIdAndNameAndIdNot(parent.getId(), name, id);

        if (duplicated) {
            throw new CustomException(ErrorCode.REGION_DUPLICATE_NAME);
        }
    }
}

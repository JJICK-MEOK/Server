package com.jjikmeok.app.domain.region.service;

import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.dto.request.RegionRequest;
import com.jjikmeok.app.domain.region.dto.response.RegionResponse;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionServiceImplTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ActivityRepository activityRepository;

    private RegionServiceImpl regionService;

    @BeforeEach
    void setUp() {
        regionService = new RegionServiceImpl(regionRepository, activityRepository);
    }

    @Test
    void getRegions_withoutParentId_returnsTopLevelRegions() {
        Region seoul = region(1L, "서울", RegionDepth.PROVINCE, null);
        Region gyeonggi = region(2L, "경기", RegionDepth.PROVINCE, null);
        when(regionRepository.findByParentIdIsNull()).thenReturn(List.of(seoul, gyeonggi));

        List<RegionResponse> responses = regionService.getRegions(null);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(RegionResponse::name).containsExactly("서울", "경기");
        verify(regionRepository).findByParentIdIsNull();
    }

    @Test
    void getRegions_withParentId_returnsSubRegions() {
        Region parent = region(1L, "서울", RegionDepth.PROVINCE, null);
        Region gangnam = region(10L, "강남구", RegionDepth.DISTRICT, parent);
        when(regionRepository.findByParentId(1L)).thenReturn(List.of(gangnam));

        List<RegionResponse> responses = regionService.getRegions(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().parentId()).isEqualTo(1L);
        assertThat(responses.getFirst().depth()).isEqualTo(RegionDepth.DISTRICT);
        verify(regionRepository).findByParentId(1L);
    }

    @Test
    void getRegionById_whenNotFound_throwsRegionNotFound() {
        when(regionRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.getRegionById(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void createRegion_createsProvinceWithTrimmedName() {
        RegionRequest request = new RegionRequest(null, " 서울 ", RegionDepth.PROVINCE);
        when(regionRepository.existsByParentIsNullAndName("서울")).thenReturn(false);
        when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
            Region savedRegion = invocation.getArgument(0);
            setId(savedRegion, 1L);
            return savedRegion;
        });

        RegionResponse response = regionService.createRegion(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.parentId()).isNull();
        assertThat(response.name()).isEqualTo("서울");
        assertThat(response.depth()).isEqualTo(RegionDepth.PROVINCE);

        ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
        verify(regionRepository).save(regionCaptor.capture());
        assertThat(regionCaptor.getValue().getName()).isEqualTo("서울");
    }

    @Test
    void createRegion_createsDistrictUnderProvince() {
        Region parent = region(1L, "서울", RegionDepth.PROVINCE, null);
        RegionRequest request = new RegionRequest(1L, "강남구", RegionDepth.DISTRICT);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(regionRepository.existsByParentIdAndName(1L, "강남구")).thenReturn(false);
        when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
            Region savedRegion = invocation.getArgument(0);
            setId(savedRegion, 10L);
            return savedRegion;
        });

        RegionResponse response = regionService.createRegion(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.parentId()).isEqualTo(1L);
        assertThat(response.depth()).isEqualTo(RegionDepth.DISTRICT);
    }

    @Test
    void createRegion_whenDistrictHasNoParent_throwsParentRequired() {
        RegionRequest request = new RegionRequest(null, "강남구", RegionDepth.DISTRICT);

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_PARENT_REQUIRED);
        verify(regionRepository, never()).save(any());
    }

    @Test
    void createRegion_whenProvinceHasParent_throwsParentNotAllowed() {
        Region parent = region(1L, "서울", RegionDepth.PROVINCE, null);
        RegionRequest request = new RegionRequest(1L, "강남구", RegionDepth.PROVINCE);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(parent));

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_PARENT_NOT_ALLOWED);
        verify(regionRepository, never()).save(any());
    }

    @Test
    void createRegion_whenDistrictParentIsNotProvince_throwsInvalidParentDepth() {
        Region parent = region(10L, "강남구", RegionDepth.DISTRICT, null);
        RegionRequest request = new RegionRequest(10L, "역삼동", RegionDepth.DISTRICT);
        when(regionRepository.findById(10L)).thenReturn(Optional.of(parent));

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_INVALID_PARENT_DEPTH);
        verify(regionRepository, never()).save(any());
    }

    @Test
    void createRegion_whenDuplicateNameExists_throwsDuplicateName() {
        RegionRequest request = new RegionRequest(null, "서울", RegionDepth.PROVINCE);
        when(regionRepository.existsByParentIsNullAndName("서울")).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_DUPLICATE_NAME);
        verify(regionRepository, never()).save(any());
    }

    @Test
    void createRegion_whenParentNotFound_throwsParentNotFound() {
        RegionRequest request = new RegionRequest(1L, "강남구", RegionDepth.DISTRICT);
        when(regionRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_PARENT_NOT_FOUND);
        verify(regionRepository, never()).save(any());
    }

    @Test
    void createRegion_whenSaveConflicts_throwsDuplicateName() {
        RegionRequest request = new RegionRequest(null, "서울", RegionDepth.PROVINCE);
        when(regionRepository.existsByParentIsNullAndName("서울")).thenReturn(false);
        when(regionRepository.save(any(Region.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.createRegion(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_DUPLICATE_NAME);
    }

    @Test
    void updateRegion_whenSelfParent_throwsSelfParentNotAllowed() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        RegionRequest request = new RegionRequest(1L, "서울", RegionDepth.DISTRICT);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.updateRegion(1L, request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_SELF_PARENT_NOT_ALLOWED);
    }

    @Test
    void updateRegion_updatesNameAndParent() {
        Region oldParent = region(1L, "서울", RegionDepth.PROVINCE, null);
        Region newParent = region(2L, "경기", RegionDepth.PROVINCE, null);
        Region region = region(10L, "강남구", RegionDepth.DISTRICT, oldParent);
        RegionRequest request = new RegionRequest(2L, "성남시", RegionDepth.DISTRICT);
        when(regionRepository.findById(10L)).thenReturn(Optional.of(region));
        when(regionRepository.findById(2L)).thenReturn(Optional.of(newParent));
        when(regionRepository.existsByParentIdAndNameAndIdNot(2L, "성남시", 10L)).thenReturn(false);

        RegionResponse response = regionService.updateRegion(10L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.parentId()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("성남시");
        assertThat(region.getParent()).isSameAs(newParent);
    }

    @Test
    void deleteRegion_deletesWhenNotUsed() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityRepository.existsByRegionId(1L)).thenReturn(false);

        regionService.deleteRegion(1L);

        verify(regionRepository).delete(region);
    }

    @Test
    void deleteRegion_whenActivityUsesRegion_throwsRegionInUse() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityRepository.existsByRegionId(1L)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.deleteRegion(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_IN_USE);
        verify(regionRepository, never()).delete(any());
    }

    @Test
    void deleteRegion_whenRepositoryRejectsDelete_throwsRegionInUse() {
        Region region = region(1L, "서울", RegionDepth.PROVINCE, null);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(region));
        when(activityRepository.existsByRegionId(1L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk violation")).when(regionRepository).delete(region);

        CustomException exception = assertThrows(CustomException.class,
                () -> regionService.deleteRegion(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_IN_USE);
    }

    private Region region(Long id, String name, RegionDepth depth, Region parent) {
        Region region = Region.builder()
                .parent(parent)
                .name(name)
                .depth(depth)
                .build();
        setId(region, id);
        return region;
    }

    private void setId(Region region, Long id) {
        ReflectionTestUtils.setField(region, "id", id);
    }
}

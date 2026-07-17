package com.jjikmeok.app.domain.activity.publicactivity.publish;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivityRegionResolver;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.domain.activity.service.SheetActivityTagResolver;
import com.jjikmeok.app.domain.region.entity.Region;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicActivityPublishServiceTest {

    @Test
    void publishReadyRows_processesOnlyPublicApiRows() {
        GoogleSheetsService sheetsService = mock(GoogleSheetsService.class);
        PublicActivityPublishService service = new PublicActivityPublishService(
                sheetsService,
                mock(ActivityRepository.class),
                mock(ActivityService.class),
                mock(ActivityRegionResolver.class),
                mock(ActivitySyncUtils.class),
                mock(SheetActivityTagResolver.class)
        );
        when(sheetsService.findReadyRows()).thenReturn(List.of(
                row(217, "KOPIS"),
                row(218, "문화 행사 검색어")
        ));
        doThrow(new IllegalStateException("stop after routing check"))
                .when(sheetsService).updateRow(any());

        service.publishReadyRows();

        ArgumentCaptor<DiscoverySheetRowDto> captor = ArgumentCaptor.forClass(DiscoverySheetRowDto.class);
        verify(sheetsService).updateRow(captor.capture());
        assertThat(captor.getValue().rowNumber()).isEqualTo(217);
    }

    @Test
    void publishReadyRows_mapsEnumsAndStoresSixResolvedTags() {
        GoogleSheetsService sheetsService = mock(GoogleSheetsService.class);
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        ActivityService activityService = mock(ActivityService.class);
        ActivityRegionResolver regionResolver = mock(ActivityRegionResolver.class);
        SheetActivityTagResolver tagResolver = mock(SheetActivityTagResolver.class);
        PublicActivityPublishService service = new PublicActivityPublishService(
                sheetsService,
                activityRepository,
                activityService,
                regionResolver,
                mock(ActivitySyncUtils.class),
                tagResolver
        );
        DiscoverySheetRowDto row = publishableRow(217, "KOPIS");
        List<Long> tagIds = List.of(1L, 2L, 8L, 12L, 18L, 19L);
        Region region = mock(Region.class);
        ActivityDetailResponse response = mock(ActivityDetailResponse.class);
        when(region.getId()).thenReturn(1L);
        when(response.id()).thenReturn(99L);
        when(sheetsService.findReadyRows()).thenReturn(List.of(row));
        when(regionResolver.resolve(any(), any(), any(), any())).thenReturn(region);
        when(tagResolver.resolveTagIds(any())).thenReturn(tagIds);
        when(activityService.createActivityWithTags(any(ActivityRequest.class), eq(tagIds))).thenReturn(response);

        int published = service.publishReadyRows();

        assertThat(published).isEqualTo(1);
        ArgumentCaptor<ActivityRequest> requestCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
        verify(activityService).createActivityWithTags(requestCaptor.capture(), eq(tagIds));
        assertThat(requestCaptor.getValue().category()).isEqualTo(ActivityCategory.CULTURE);
        assertThat(requestCaptor.getValue().activityType()).isEqualTo(ActivityType.EVENT);
        assertThat(requestCaptor.getValue().sourceType()).isEqualTo(SourceType.KOPIS);
        verify(activityService, never()).createActivity(any());
    }

    private DiscoverySheetRowDto row(int number, String origin) {
        List<Object> values = new ArrayList<>(Collections.nCopies(29, null));
        values.set(0, number);
        values.set(4, "발행대기");
        values.set(7, "활동 " + number);
        values.set(9, "https://example.com/" + number);
        values.set(27, origin);
        return DiscoverySheetRowDto.fromSheetRow(number + 1, values);
    }

    private DiscoverySheetRowDto publishableRow(int number, String origin) {
        List<Object> values = new ArrayList<>(Collections.nCopies(29, null));
        values.set(0, number);
        values.set(4, "발행대기");
        values.set(5, "CULTURE");
        values.set(6, "EVENT");
        values.set(7, "활동 " + number);
        values.set(8, "운영처");
        values.set(9, "https://example.com/" + number);
        values.set(11, "2026-08-01");
        values.set(12, "2026-08-02");
        values.set(13, "2026-07-01");
        values.set(14, "2026-07-31");
        values.set(16, "10000");
        values.set(17, "설명");
        values.set(21, "CALM");
        values.set(22, "HEALING");
        values.set(23, "LIGHT");
        values.set(24, "HOBBY");
        values.set(25, "OVER_ONE_YEAR");
        values.set(26, "SMALL");
        values.set(27, origin);
        values.set(28, "100");
        return DiscoverySheetRowDto.fromSheetRow(number + 1, values);
    }
}

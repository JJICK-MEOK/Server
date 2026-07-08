package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.service.ActivityTagAutoAttachService;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ActivitySyncServiceImplTest {

    @Mock private ActivityRegionResolver activityRegionResolver;
    @Mock private ExternalActivityGateway externalActivityGateway;
    @Mock private ActivityNormalizer activityNormalizer;
    @Mock private RawActivityArchiveService rawActivityArchiveService;
    @Mock private ActivityRepository activityRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private GoogleSheetsService googleSheetsService;
    @Mock private ActivityAttachmentStorageService activityAttachmentStorageService;
    @Mock private ActivityDetailEnricher activityDetailEnricher;
    @Mock private ActivityTagAutoAttachService activityTagAutoAttachService;
    @Mock private ActivitySyncUtils utils;
    @Mock private AiActivityParser aiActivityParser;
    @Mock private VectorStore vectorStore;

    private ActivitySyncServiceImpl activitySyncService;

    @BeforeEach
    void setUp() {
        activitySyncService = new ActivitySyncServiceImpl(
                activityRegionResolver,
                externalActivityGateway,
                activityNormalizer,
                rawActivityArchiveService,
                activityRepository,
                regionRepository,
                googleSheetsService,
                activityAttachmentStorageService,
                activityDetailEnricher,
                activityTagAutoAttachService,
                utils,
                aiActivityParser,
                vectorStore
        );
    }

    @Test
    void sync_whenKopisServiceKeyMissing_throwsConfigMissing() {
        assertThatThrownBy(() -> activitySyncService.sync(SourceType.KOPIS, null, 1))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ACTIVITY_SYNC_CONFIG_MISSING));
    }
}

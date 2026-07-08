package com.jjikmeok.app.domain.activity;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.collector.DiscoveryCollectorService;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.privateactivity.publish.DiscoveryPublishService;
import com.jjikmeok.app.domain.activity.privateactivity.sheets.GoogleSheetsService;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncService;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "app.discovery.scheduler.enabled=false",
        "app.discovery.analysis.max-ai-analysis-per-run=3",
        "app.activity-sync.default-max-pages=1",
        "app.activity-sync.kopis.max-pages=1",
        "app.activity-sync.exhibition.max-pages=1",
        "app.activity-sync.seoul-culture.max-pages=1",
        "app.activity-sync.seoul-reservation.max-pages=1",
        "app.discovery.sheets.enabled=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActivityIngestionExternalE2ETest {

    @Autowired
    private ActivitySyncService activitySyncService;

    @Autowired
    private DiscoveryCollectorService discoveryCollectorService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private DiscoveryPublishService discoveryPublishService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private VectorStore vectorStore;

    @BeforeAll
    void enabledOnlyWhenExplicitlyRequested() {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("JJIKMEOK_EXTERNAL_E2E")),
                "Set JJIKMEOK_EXTERNAL_E2E=true to run real API/Sheets/DB tests."
        );
        allowCurrentSourceTypes();
    }

    private void allowCurrentSourceTypes() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.activities') IS NOT NULL THEN
                        ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_source_type_check;
                        ALTER TABLE activities ADD CONSTRAINT activities_source_type_check
                            CHECK (source_type IN (
                                'KOPIS',
                                'EXHIBITION',
                                'SEOUL_CULTURE',
                                'SEOUL_RESERVATION',
                                'DISCOVERY',
                                'URL_MANUAL'
                            ));
                    END IF;

                    IF to_regclass('public.raw_activities') IS NOT NULL THEN
                        ALTER TABLE raw_activities DROP CONSTRAINT IF EXISTS raw_activities_source_type_check;
                        ALTER TABLE raw_activities ADD CONSTRAINT raw_activities_source_type_check
                            CHECK (source_type IN (
                                'KOPIS',
                                'EXHIBITION',
                                'SEOUL_CULTURE',
                                'SEOUL_RESERVATION',
                                'DISCOVERY',
                                'URL_MANUAL'
                            ));
                    END IF;
                END $$;
                """);
    }

    @Test
    @Order(1)
    void publicFourSourcesSyncToDbAndSheets() {
        for (SourceType sourceType : SourceType.publicApiSources()) {
            ActivitySyncResponse response = activitySyncService.sync(sourceType, null, 1);

            assertThat(response.getRawSavedCount())
                    .as(sourceType + " raw payload should be archived")
                    .isPositive();
            assertThat(response.getActivitySavedCount() + response.getDuplicatedCount())
                    .as(sourceType + " should create or match at least one activity")
                    .isPositive();
        }
    }

    @Test
    @Order(2)
    void discoveryCollectsAnalyzesWritesSheetAndPublishesReadyRow() {
        assertThat(googleSheetsService.findReadyRows())
                .as("Publish test is isolated only when the sheet has no pre-existing READY rows")
                .isEmpty();

        long beforeActivityCount = activityRepository.count();
        List<DiscoverySheetRowDto> rows = discoveryCollectorService.run(ActivityCategory.CULTURE, 1, 3);

        assertThat(rows)
                .as("Discovery should collect at least one candidate from 3 search results")
                .isNotEmpty();

        DiscoverySheetRowDto row = rows.stream()
                .filter(candidate -> candidate.sourceUrl() != null && !candidate.sourceUrl().isBlank())
                .findFirst()
                .orElseThrow();

        assertThat(row.activityType())
                .as("AI analysis should fill activityType")
                .isNotNull();
        assertThat(row.category())
                .as("AI analysis should fill category")
                .isNotNull();

        googleSheetsService.updateRow(row.withStatus(DiscoverySheetStatus.READY, null));

        int publishedCount = discoveryPublishService.publishReadyRows();

        assertThat(publishedCount)
                .as("At least the test-created READY row should be published")
                .isPositive();
        assertThat(activityRepository.count())
                .as("Publishing should create a DB activity")
                .isGreaterThan(beforeActivityCount);
    }
}

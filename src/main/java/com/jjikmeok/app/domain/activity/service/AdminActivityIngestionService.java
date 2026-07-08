package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.enums.PublicActivitySourceType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.collector.DiscoveryCollectorService;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.privateactivity.publish.DiscoveryPublishService;
import com.jjikmeok.app.domain.activity.publicactivity.publish.PublicActivityPublishService;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminActivityIngestionService {

    private final ActivitySyncService activitySyncService;
    private final DiscoveryCollectorService discoveryCollectorService;
    private final DiscoveryPublishService discoveryPublishService;
    private final PublicActivityPublishService publicActivityPublishService;

    @Value("${app.discovery.scheduler.keywords-per-run:${app.discovery.scheduler.keyword-limit:10}}")
    private int defaultKeywordLimit;

    @Value("${app.discovery.search.results-per-keyword:${app.discovery.search.result-limit:10}}")
    private int defaultResultLimit;

    public void syncAllPublicSources() {
        activitySyncService.syncAllSources();
    }

    public ActivitySyncResponse syncPublicSource(PublicActivitySourceType sourceType, Integer maxPages) {
        SourceType publicSourceType = sourceType.toSourceType();
        return activitySyncService.sync(publicSourceType, null, maxPages);
    }

    public List<DiscoverySheetRowDto> collectDiscoveryActivities(Integer keywordLimit, Integer resultLimit) {
        return discoveryCollectorService.runAll(
                keywordLimit == null ? defaultKeywordLimit : keywordLimit,
                resultLimit == null ? defaultResultLimit : resultLimit
        );
    }

    public int publishDiscoveryActivities() {
        return discoveryPublishService.publishReadyRows();
    }

    public int publishPublicActivities() {
        return publicActivityPublishService.publishReadyRows();
    }
}

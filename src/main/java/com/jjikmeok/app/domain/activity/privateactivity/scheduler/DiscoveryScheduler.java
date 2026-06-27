package com.jjikmeok.app.domain.activity.privateactivity.scheduler;

import com.jjikmeok.app.domain.activity.privateactivity.collector.DiscoveryCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.discovery.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class DiscoveryScheduler {

    private final DiscoveryCollectorService discoveryCollectorService;

    @Value("${app.discovery.scheduler.keywords-per-run:${app.discovery.scheduler.keyword-limit:10}}")
    private int keywordLimit;

    @Value("${app.discovery.search.results-per-keyword:${app.discovery.search.result-limit:10}}")
    private int resultLimit;

    @Scheduled(cron = "${app.discovery.scheduler.cron:0 0 3 ? * MON,FRI}", zone = "Asia/Seoul")
    public void runDiscoveryPipeline() {
        log.info("[디스커버리] 수집 스케줄 실행을 시작합니다. keywordLimit={}, resultLimit={}", keywordLimit, resultLimit);
        int rows = discoveryCollectorService.runAll(keywordLimit, resultLimit).size();
        if (rows < keywordLimit && keywordLimit < 20) {
            int expandedKeywordLimit = 20;
            log.info("[디스커버리] 후보 수가 부족하여 키워드 범위를 확장합니다. keywordLimit={}, nextKeywordLimit={}", rows, expandedKeywordLimit);
            rows += discoveryCollectorService.runAll(expandedKeywordLimit, resultLimit).size();
        }
        log.info("[디스커버리] 수집 스케줄 실행을 완료했습니다. 저장된 후보 수={}", rows);
    }
}

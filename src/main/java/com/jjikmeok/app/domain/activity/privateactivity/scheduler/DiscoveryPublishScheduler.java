package com.jjikmeok.app.domain.activity.privateactivity.scheduler;

import com.jjikmeok.app.domain.activity.privateactivity.publish.DiscoveryPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.discovery.publish.enabled", havingValue = "true", matchIfMissing = false)
public class DiscoveryPublishScheduler {

    private final DiscoveryPublishService discoveryPublishService;

    @Scheduled(fixedDelayString = "${app.discovery.publish.fixed-delay-ms:300000}")
    public void runDiscoveryPublish() {
        log.info("[Discovery 발행] 시트 발행 스캔을 시작합니다.");
        int discoveryProcessed = discoveryPublishService.publishReadyRows();
        log.info("[Discovery 발행] 시트 발행 스캔을 완료했습니다. processed={}", discoveryProcessed);
    }
}

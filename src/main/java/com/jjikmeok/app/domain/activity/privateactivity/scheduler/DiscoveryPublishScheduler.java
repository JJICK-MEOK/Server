package com.jjikmeok.app.domain.activity.privateactivity.scheduler;

import com.jjikmeok.app.domain.activity.privateactivity.publish.DiscoveryPublishService;
import com.jjikmeok.app.domain.activity.publicactivity.publish.PublicActivityPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.discovery.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class DiscoveryPublishScheduler {

    private final DiscoveryPublishService discoveryPublishService;
    private final PublicActivityPublishService publicActivityPublishService;

    @Scheduled(fixedDelay = 300000)
    public void runDiscoveryPublish() {
        log.info("[발행] 시트 발행 스캔을 시작합니다.");
        int discoveryProcessed = discoveryPublishService.publishReadyRows();
        int publicProcessed = publicActivityPublishService.publishReadyRows();
        log.info("[발행] 시트 발행 스캔을 완료했습니다. discovery={}, public={}", discoveryProcessed, publicProcessed);
    }
}

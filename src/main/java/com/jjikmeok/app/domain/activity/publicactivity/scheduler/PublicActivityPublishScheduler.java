package com.jjikmeok.app.domain.activity.publicactivity.scheduler;

import com.jjikmeok.app.domain.activity.publicactivity.publish.PublicActivityPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.activity-sync.publish.enabled", havingValue = "true", matchIfMissing = false)
public class PublicActivityPublishScheduler {

    private final PublicActivityPublishService publicActivityPublishService;

    @Scheduled(fixedDelayString = "${app.activity-sync.publish.fixed-delay-ms:300000}")
    public void runPublicActivityPublish() {
        log.info("[공공 발행] 시트 발행 스캔을 시작합니다.");
        int processed = publicActivityPublishService.publishReadyRows();
        log.info("[공공 발행] 시트 발행 스캔을 완료했습니다. processed={}", processed);
    }
}

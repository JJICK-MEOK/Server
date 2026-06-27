package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySyncScheduler {

    private final ActivitySyncService activitySyncService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void syncDailySources() {
        log.info("[ActivitySync] 공공 API 일일 동기화를 시작합니다.");
        long startTime = System.currentTimeMillis();

        sync(SourceType.KOPIS, SourceType.EXHIBITION, SourceType.SEOUL_CULTURE, SourceType.SEOUL_RESERVATION);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[ActivitySync] 공공 API 일일 동기화를 완료했습니다. 소요시간={}ms", duration);
    }

    private void sync(SourceType... sourceTypes) {
        for (SourceType sourceType : sourceTypes) {
            try {
                log.info("[ActivitySync] {} 동기화를 시작합니다.", sourceType);
                activitySyncService.sync(sourceType, null);
                log.info("[ActivitySync] {} 동기화를 완료했습니다.", sourceType);
            } catch (Exception e) {
                log.error("[ActivitySync] {} 동기화 중 오류가 발생했습니다. {}", sourceType, e.getMessage(), e);
            }
        }
    }
}

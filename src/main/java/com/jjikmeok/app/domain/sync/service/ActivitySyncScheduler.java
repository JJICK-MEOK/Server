package com.jjikmeok.app.domain.sync.service;

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
        log.info("⏰ [정기 스케줄러] 7개 외부 데이터 API 통합 일일 배치 동기화를 시작합니다.");
        long startTime = System.currentTimeMillis(); // 전체 배치 시작 시간 측정

        sync(SourceType.TOUR_API, SourceType.KOPIS, SourceType.EXHIBITION, SourceType.YOUTH_CONTENT,
                SourceType.SEOUL_CULTURE, SourceType.VOLUNTEER_1365, SourceType.SEOUL_RESERVATION);

        long duration = System.currentTimeMillis() - startTime; // 전체 배치 종료 시간 계산
        log.info("🏁 [정기 스케줄러 종료] 전체 API 동기화 스케줄이 무사히 완료되었습니다. 총 소요시간: {}ms", duration);
    }

    private void sync(SourceType... sourceTypes) {
        for (SourceType sourceType : sourceTypes) {
            try {
                // 🌟 1. 개별 API 수집 시작 로그 추가
                log.info("🔄 [{}] 데이터 동기화 파이프라인 프로세싱 가동...", sourceType);

                activitySyncService.sync(sourceType, null);

                // 🌟 2. 개별 API 수집 성공 로그 추가
                log.info("✅ [{}] 동기화 프로세스 안전 완료", sourceType);
            } catch (Exception e) {
                // 예외 격리 구조 유지 및 스택 트레이스 전체 출력하도록 수정
                log.error("❌ 활동 동기화 작업 치명적 실패. 수집출처={}, 에러내용={}", sourceType, e.getMessage(), e);
            }
        }
    }
}
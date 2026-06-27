package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;

public interface ActivitySyncService {

    // 🌟 5개 오픈 API 일괄 배치 동기화 추상 매서드 선언 추가
    void syncAllSources();

    ActivitySyncResponse sync(SourceType sourceType, ActivityCategory categoryOverride);

    ActivitySyncResponse sync(SourceType sourceType, ActivityCategory categoryOverride, Integer maxPagesOverride);
}
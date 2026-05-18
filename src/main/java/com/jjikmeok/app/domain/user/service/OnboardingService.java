package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.dto.request.OnboardingReq;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;

public interface OnboardingService {

    OnboardingRes completeOnboarding(Long userId, OnboardingReq request);
}

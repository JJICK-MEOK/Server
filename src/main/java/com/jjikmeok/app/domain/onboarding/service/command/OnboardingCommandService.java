package com.jjikmeok.app.domain.onboarding.service.command;

import com.jjikmeok.app.domain.onboarding.dto.request.OnboardingReq;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;

public interface OnboardingCommandService {

    OnboardingRes completeOnboarding(Long userId, OnboardingReq request);
}

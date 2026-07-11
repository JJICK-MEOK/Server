package com.jjikmeok.app.domain.onboarding.service.query;

import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingPreferenceTagRes;

import java.util.List;

public interface OnboardingQueryService {

    OnboardingRes getOnboarding(Long userId);

    List<OnboardingPreferenceTagRes> getPreferenceTagsForEdit(Long userId);
}

package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.dto.response.OnboardingPreferenceTagResponse;

import java.util.List;

public interface OnboardingQueryService {

    OnboardingRes getOnboarding(Long userId);

    List<OnboardingPreferenceTagResponse> getPreferenceTagsForEdit(Long userId);
}

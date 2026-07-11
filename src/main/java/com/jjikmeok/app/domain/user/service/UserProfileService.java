package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;

public interface UserProfileService {

    UserProfileCreateRes createProfile(Long userId, UserProfileCreateReq request);

    UserProfileMeRes getMyProfile(Long userId);
}

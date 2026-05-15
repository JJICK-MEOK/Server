package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;

public interface UserProfileService {

    UserProfileCreateRes createProfile(Long userId, UserProfileCreateReq request);
}

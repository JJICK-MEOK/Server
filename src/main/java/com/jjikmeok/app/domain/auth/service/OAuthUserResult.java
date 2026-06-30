package com.jjikmeok.app.domain.auth.service;

import com.jjikmeok.app.domain.user.entity.User;

record OAuthUserResult(
        User user,
        boolean newMember
) {
}

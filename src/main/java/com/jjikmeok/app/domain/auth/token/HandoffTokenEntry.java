package com.jjikmeok.app.domain.auth.token;

import java.time.Instant;

public record HandoffTokenEntry(

        Long memberId,

        boolean isNewMember,

        Instant createdAt
) {
}

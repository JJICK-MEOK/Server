package com.jjikmeok.app.domain.activity.enums;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public boolean isApproved() {
        return this == APPROVED;
    }
}

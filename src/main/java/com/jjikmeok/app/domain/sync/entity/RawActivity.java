package com.jjikmeok.app.domain.sync.entity;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "raw_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawActivity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "request_url", nullable = false, length = 1000)
    private String requestUrl;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    public static RawActivity create(SourceType sourceType, String externalId, String requestUrl, String contentType, String payload) {
        RawActivity rawActivity = new RawActivity();
        rawActivity.sourceType = sourceType;
        rawActivity.externalId = externalId;
        rawActivity.requestUrl = requestUrl;
        rawActivity.contentType = contentType;
        rawActivity.payload = payload;
        return rawActivity;
    }
}

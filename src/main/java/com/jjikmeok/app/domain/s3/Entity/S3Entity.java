package com.jjikmeok.app.domain.s3.Entity;

import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class S3Entity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;

    private String imageKey;
}
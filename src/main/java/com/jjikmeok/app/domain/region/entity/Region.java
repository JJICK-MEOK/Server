package com.jjikmeok.app.domain.region.entity;

import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    @Column(nullable = false, length = 50)
    private String name;

    // Integer 타입에서 Enum 타입으로 변경 및 DB 매핑 설정
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegionDepth depth;

    @Builder
    public Region(Region parent, String name, RegionDepth depth) {
        this.parent = parent;
        this.name = name;
        this.depth = depth; // Enum 적용
    }

    public void update(Region parent, String name, RegionDepth depth) {
        this.parent = parent;
        this.name = name;
        this.depth = depth; // Enum 적용
    }
}
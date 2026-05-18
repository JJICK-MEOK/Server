package com.jjikmeok.app.domain.tag.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tag entity.
 * <p>
 * 주제, 취향, 활동 카테고리를 하나의 테이블에서 관리하는 공용 태그 엔티티다.
 *
 * @author Codex
 * @version 1.0
 * @since 2026-05-16
 */
@Entity
@Getter
@Table(name = "tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TagType type;

    public static Tag create(String name, TagType type) {
        Tag tag = new Tag();
        tag.name = name;
        tag.type = type;
        return tag;
    }

    public void update(String name, TagType type) {
        this.name = name;
        this.type = type;
    }
}

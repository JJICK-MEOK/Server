package com.jjikmeok.app.domain.tag.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tags_name_type", columnNames = {"name", "type"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
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

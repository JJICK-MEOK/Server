package com.jjikmeok.app.domain.personalization.entity;

import com.jjikmeok.app.domain.tag.entity.TagType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tags_name_type",
                        columnNames = {"name", "type"}
                )
        }
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 태그명
     * 예: 운동, 감성적인, 소규모
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /*
     * ACTIVITY_CATEGORY
     * TOPIC_CATEGORY
     * PREFERENCE_TAG
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TagType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Tag(String name, TagType type) {
        this.name = name;
        this.type = type;
    }

    public static Tag of(String name, TagType type) {
        return new Tag(name, type);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateType(TagType type) {
        this.type = type;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

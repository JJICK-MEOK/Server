package com.jjikmeok.app.domain.personalization.entity;

import com.jjikmeok.app.domain.tag.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "tag_preference_vectors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tag_preference_vectors_tag_id",
                        columnNames = "tag_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagPreferenceVector extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tag_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_tag_preference_vectors_tag"
            )
    )
    private Tag tag;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = PreferenceVectorConstants.DIMENSION)
    @Column(
            name = "embedding",
            nullable = false,
            columnDefinition = "vector(14)"
    )
    @Getter(AccessLevel.NONE)
    private float[] embedding;

    @Column(name = "vector_version", nullable = false)
    private int vectorVersion;

    private TagPreferenceVector(
            Tag tag,
            float[] embedding
    ) {
        if (tag == null) {
            throw new IllegalArgumentException(
                    "태그는 null일 수 없습니다."
            );
        }

        this.tag = tag;
        this.embedding =
                PreferenceVectorValidator.validateAndCopy(embedding);
        this.vectorVersion =
                PreferenceVectorConstants.CURRENT_VERSION;
    }

    public static TagPreferenceVector create(
            Tag tag,
            float[] embedding
    ) {
        return new TagPreferenceVector(tag, embedding);
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding =
                PreferenceVectorValidator.validateAndCopy(embedding);
        this.vectorVersion =
                PreferenceVectorConstants.CURRENT_VERSION;
    }

    public float[] getEmbeddingCopy() {
        return embedding.clone();
    }
}

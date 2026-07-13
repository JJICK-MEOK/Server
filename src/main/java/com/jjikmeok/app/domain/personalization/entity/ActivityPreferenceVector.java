package com.jjikmeok.app.domain.personalization.entity;

import com.jjikmeok.app.domain.activity.entity.Activity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(
        name = "activity_preference_vectors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_activity_preference_vectors_activity_id",
                        columnNames = "activity_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityPreferenceVector extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 하나의 활동당 최종 취향 벡터 하나만 저장한다.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_activity_preference_vectors_activity"
            )
    )
    private Activity activity;

    @Convert(converter = FloatArrayJsonConverter.class)
    @Column(
            name = "embedding",
            nullable = false,
            columnDefinition = "JSON"
    )
    @Getter(AccessLevel.NONE)
    private float[] embedding;

    @Column(name = "vector_version", nullable = false)
    private int vectorVersion;

    private ActivityPreferenceVector(
            Activity activity,
            float[] embedding
    ) {
        if (activity == null) {
            throw new IllegalArgumentException(
                    "활동은 null일 수 없습니다."
            );
        }

        this.activity = activity;
        this.embedding =
                PreferenceVectorValidator.validateAndCopy(embedding);
        this.vectorVersion =
                PreferenceVectorConstants.CURRENT_VERSION;
    }

    public static ActivityPreferenceVector create(
            Activity activity,
            float[] embedding
    ) {
        return new ActivityPreferenceVector(
                activity,
                embedding
        );
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

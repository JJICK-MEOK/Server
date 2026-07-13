package com.jjikmeok.app.domain.personalization.entity;

import com.jjikmeok.app.domain.personalization.entity.BaseTimeEntity;
import com.jjikmeok.app.domain.personalization.entity.PreferenceVectorConstants;
import com.jjikmeok.app.domain.personalization.entity.PreferenceVectorValidator;
import com.jjikmeok.app.domain.user.entity.User;
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
        name = "user_preference_vectors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_preference_vectors_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceVector extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_user_preference_vectors_user"
            )
    )
    private User user;

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

    private UserPreferenceVector(
            User user,
            float[] embedding
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자는 null일 수 없습니다."
            );
        }

        this.user = user;
        this.embedding =
                PreferenceVectorValidator.validateAndCopy(embedding);
        this.vectorVersion =
                PreferenceVectorConstants.CURRENT_VERSION;
    }

    public static UserPreferenceVector create(
            User user,
            float[] embedding
    ) {
        return new UserPreferenceVector(user, embedding);
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

package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.personalization.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalizationRepository extends JpaRepository<UserOnboardingTag, Long> {
    @Query(
            value = """
                    SELECT t.name
                    FROM user_onboarding uo
                    JOIN user_onboarding_tags uot
                        ON uot.user_onboarding_id = uo.id
                    JOIN tags t
                        ON t.id = uot.tag_id
                    WHERE uo.user_id = :userId
                    """,
            nativeQuery = true
    )
    List<String> findTagNamesByUserId(@Param("userId") Long userId);
}

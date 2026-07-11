package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.personalization.entity.UserPreferenceVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceVectorRepository extends JpaRepository<UserPreferenceVector, Long> {

    Optional<UserPreferenceVector> findByUserId(Long userId);
}

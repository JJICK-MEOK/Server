package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);

    boolean existsByNickname(String nickname);

    Optional<UserProfile> findByUserId(Long userId);
}

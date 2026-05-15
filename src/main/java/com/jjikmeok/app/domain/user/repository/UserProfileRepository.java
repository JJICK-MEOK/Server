package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);

    boolean existsByNickname(String nickname);
}

package com.jjikmeok.app.domain.user.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_provider", columnNames = {"auth_provider", "provider_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String email;

    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_id", length = 50)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted;

    public static User createForSignup(String email, String passwordHash) {
        User user = new User();
        user.email = email;
        user.authProvider = AuthProvider.LOCAL;
        user.providerId = null;
        user.passwordHash = passwordHash;
        user.onboardingCompleted = false;
        user.role = UserRole.USER;

        return user;
    }

    public static User createForOAuth2(
            String email,
            AuthProvider provider,
            String providerId
    ) {
        User user = new User();
        user.email = email;
        user.authProvider = provider;
        user.providerId = providerId;
        user.passwordHash = null;
        user.onboardingCompleted = false;
        user.role = UserRole.USER;

        return user;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }
}
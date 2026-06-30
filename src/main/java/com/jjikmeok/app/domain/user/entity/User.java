package com.jjikmeok.app.domain.user.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.*;
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

    @Column(nullable = true, length = 150)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationStatus registrationStatus;

    public static User createForSignup(String email, String passwordHash) {
        User user = new User();
        user.email = email;
        user.authProvider = AuthProvider.LOCAL;
        user.providerId = null;
        user.passwordHash = passwordHash;
        user.registrationStatus = RegistrationStatus.NOT_STARTED;
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
        user.registrationStatus = RegistrationStatus.NOT_STARTED;
        user.role = UserRole.USER;

        return user;
    }

    public void completeProfile() {
        this.registrationStatus = RegistrationStatus.PROFILE_COMPLETED;
    }

    public void completeOnboarding() {
        this.registrationStatus = RegistrationStatus.ONBOARDING_COMPLETED;
    }
}

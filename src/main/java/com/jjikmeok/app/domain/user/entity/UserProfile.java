package com.jjikmeok.app.domain.user.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_profiles_user_id", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_user_profiles_nickname", columnNames = "nickname")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    private static final String DEFAULT_PROFILE_IMAGE_URL = "";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileGender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "service_terms_agreed", nullable = false)
    private Boolean serviceTermsAgreed;

    @Column(name = "privacy_policy_agreed", nullable = false)
    private Boolean privacyPolicyAgreed;

    @Column(name = "marketing_agreed", nullable = false)
    private Boolean marketingAgreed;

    public static UserProfile create(
            User user,
            String nickname,
            LocalDate birthDate,
            ProfileGender gender,
            ProfileStatus status,
            Boolean serviceTermsAgreed,
            Boolean privacyPolicyAgreed,
            Boolean marketingAgreed
    ) {
        UserProfile userProfile = new UserProfile();
        userProfile.user = user;
        userProfile.nickname = nickname;
        userProfile.birthDate = birthDate;
        userProfile.gender = gender;
        userProfile.status = status;
        userProfile.profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;
        userProfile.serviceTermsAgreed = serviceTermsAgreed;
        userProfile.privacyPolicyAgreed = privacyPolicyAgreed;
        userProfile.marketingAgreed = marketingAgreed != null ? marketingAgreed : false;
        return userProfile;
    }
}

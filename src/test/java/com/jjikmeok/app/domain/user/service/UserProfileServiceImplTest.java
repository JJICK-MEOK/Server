package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileTagProjection;
import com.jjikmeok.app.domain.user.entity.ProfileGender;
import com.jjikmeok.app.domain.user.entity.ProfileStatus;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserProfile;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileServiceImpl userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(userRepository, userProfileRepository);
    }

    @Test
    void getMyProfile_returnsProfileAndLimitsTagsToFive() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile()));
        when(userProfileRepository.findTagProjectionsByUserId(1L)).thenReturn(profileTags());

        UserProfileMeRes response = userProfileService.getMyProfile(1L);

        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(response.tags()).hasSize(5);
        assertThat(response.tags().getFirst().id()).isEqualTo(1L);
        assertThat(response.tags().getFirst().groupType()).isEqualTo(TagGroupType.MOOD);
    }

    @Test
    void getMyProfile_requiresAuthenticatedUser() {
        assertThatThrownBy(() -> userProfileService.getMyProfile(null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void getMyProfile_throwsWhenProfileDoesNotExist() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    private List<UserProfileTagProjection> profileTags() {
        return List.of(
                tag(1L, "편안한", TagType.PREFERENCE_TAG, TagGroupType.MOOD),
                tag(2L, "힐링", TagType.PREFERENCE_TAG, TagGroupType.MOOD),
                tag(3L, "입문", TagType.PREFERENCE_TAG, TagGroupType.INTENSITY),
                tag(4L, "가볍게", TagType.PREFERENCE_TAG, TagGroupType.INTENSITY),
                tag(5L, "취미", TagType.PREFERENCE_TAG, TagGroupType.PURPOSE),
                tag(6L, "운동 / 액티비티", TagType.TOPIC_CATEGORY, null)
        );
    }

    private UserProfile userProfile() {
        User user = User.createForSignup("tester@example.com", "password");
        UserProfile userProfile = UserProfile.create(
                user,
                "tester",
                LocalDate.of(1999, 1, 1),
                ProfileGender.MALE,
                ProfileStatus.STUDENT,
                true,
                true,
                false
        );
        ReflectionTestUtils.setField(userProfile, "profileImageUrl", "https://example.com/profile.png");
        return userProfile;
    }

    private UserProfileTagProjection tag(
            Long id,
            String name,
            TagType type,
            TagGroupType tagGroupType
    ) {
        return new UserProfileTagProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public TagType getType() {
                return type;
            }

            @Override
            public TagGroupType getTagGroupType() {
                return tagGroupType;
            }
        };
    }
}

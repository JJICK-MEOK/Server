package com.jjikmeok.app.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;
import com.jjikmeok.app.domain.user.service.UserProfileService;
import com.jjikmeok.app.global.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserProfileController(userProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Test
    void getMyProfile_returnsProfileAndTags() throws Exception {
        when(userProfileService.getMyProfile(null)).thenReturn(myProfileResponse());

        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("마이찍먹 사용자 프로필 조회 성공"))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.data.tags[0].id").value(1L))
                .andExpect(jsonPath("$.data.tags[0].name").value("입문"))
                .andExpect(jsonPath("$.data.tags[0].type").value("PREFERENCE_TAG"))
                .andExpect(jsonPath("$.data.tags[0].groupType").value("INTENSITY"));

        verify(userProfileService).getMyProfile(null);
    }

    private UserProfileMeRes myProfileResponse() {
        return new UserProfileMeRes(
                "tester",
                "https://example.com/profile.png",
                List.of(new UserProfileMeRes.TagResponse(
                        1L,
                        "입문",
                        TagType.PREFERENCE_TAG,
                        TagGroupType.INTENSITY
                ))
        );
    }
}

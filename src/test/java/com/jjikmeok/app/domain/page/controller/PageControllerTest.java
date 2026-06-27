package com.jjikmeok.app.domain.page.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityImageItemResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.page.service.PageService;
import com.jjikmeok.app.global.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 28, 10, 0);

    @Mock
    private PageService pageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = standaloneSetup(new PageController(pageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getHomePage_returnsScreenSections() throws Exception {
        when(pageService.getHomePage(null, 5)).thenReturn(homePageResponse());

        mockMvc.perform(get("/api/v1/pages/home").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("홈 페이지 조회 성공"))
                .andExpect(jsonPath("$.data.user.nickname").value("tester"))
                .andExpect(jsonPath("$.data.user.profileImageUrl").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.data.recommendedActivities[0].hashtags.length()").value(2))
                .andExpect(jsonPath("$.data.recommendedActivities[0].deadline").value(3))
                .andExpect(jsonPath("$.data.closingSoonActivities[0].deadline").value(3));

        verify(pageService).getHomePage(null, 5);
    }

    @Test
    void getCategoryPage_passesFilters() throws Exception {
        when(pageService.getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10))
                .thenReturn(categoryPageResponse());

        mockMvc.perform(get("/api/v1/pages/category")
                        .param("type", "PROGRAM")
                        .param("category", "CRAFT")
                        .param("sort", "deadline")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 페이지 조회 성공"))
                .andExpect(jsonPath("$.data.selectedType").value("PROGRAM"))
                .andExpect(jsonPath("$.data.typeOptions[0].label").value("전체"))
                .andExpect(jsonPath("$.data.categoryOptions[1].label").value("운동 / 액티비티"))
                .andExpect(jsonPath("$.data.activities[0].category").value("CRAFT"))
                .andExpect(jsonPath("$.data.activities[0].hashtags.length()").value(2));

        verify(pageService).getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10);
    }

    @Test
    void getCustomPage_returnsTasteProfile() throws Exception {
        when(pageService.getCustomPage(null, null)).thenReturn(customPageResponse());

        mockMvc.perform(get("/api/v1/pages/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("맞춤 페이지 조회 성공"))
                .andExpect(jsonPath("$.data.tasteProfile.title").value("추천 활동"));

        verify(pageService).getCustomPage(null, null);
    }

    @Test
    void getDetailPage_returnsDisplayFields() throws Exception {
        when(pageService.getDetailPage(null, 1L)).thenReturn(detailPageResponse());

        mockMvc.perform(get("/api/v1/pages/detail/{activityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상세 페이지 조회 성공"))
                .andExpect(jsonPath("$.data.organizer").value("운영기관"))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.data.hashtags.length()").value(3))
                .andExpect(jsonPath("$.data.deadline").value(3));

        verify(pageService).getDetailPage(null, 1L);
    }

    private ActivityHomePageResponse homePageResponse() {
        return new ActivityHomePageResponse(
                new ActivityHomePageResponse.UserResponse("tester", "https://example.com/profile.png"),
                List.of(card()),
                List.of(card())
        );
    }

    private ActivityCategoryPageResponse categoryPageResponse() {
        return new ActivityCategoryPageResponse(
                "프로그램",
                ActivityType.PROGRAM,
                ActivityCategory.CRAFT,
                "deadline",
                1L,
                List.of(
                        new ActivityFilterOptionResponse("", "전체", false),
                        new ActivityFilterOptionResponse("PROGRAM", "프로그램", true),
                        new ActivityFilterOptionResponse("ONE_DAY", "원데이", false),
                        new ActivityFilterOptionResponse("EVENT", "행사·강연", false),
                        new ActivityFilterOptionResponse("CLUB", "동아리", false)
                ),
                List.of(
                        new ActivityFilterOptionResponse("", "전체", false),
                        new ActivityFilterOptionResponse("SPORTS", "운동 / 액티비티", false),
                        new ActivityFilterOptionResponse("CULTURE", "문화 / 예술", false),
                        new ActivityFilterOptionResponse("CRAFT", "공예 / 만들기", true),
                        new ActivityFilterOptionResponse("COOKING", "요리 / 베이킹", false),
                        new ActivityFilterOptionResponse("PHOTO_VIDEO", "사진 / 영상", false),
                        new ActivityFilterOptionResponse("HUMANITIES", "책 / 글", false),
                        new ActivityFilterOptionResponse("TRAVEL", "여행 / 탐방", false),
                        new ActivityFilterOptionResponse("LANGUAGE", "언어 / 해외", false),
                        new ActivityFilterOptionResponse("VOLUNTEER", "봉사활동", false),
                        new ActivityFilterOptionResponse("CAREER", "성장 / 커리어", false)
                ),
                List.of(
                        new ActivityFilterOptionResponse("recommended", "추천순", false),
                        new ActivityFilterOptionResponse("popular", "인기순", false),
                        new ActivityFilterOptionResponse("deadline", "마감순", true)
                ),
                List.of(card())
        );
    }

    private ActivityCustomPageResponse customPageResponse() {
        return new ActivityCustomPageResponse(
                "tester",
                new ActivityCustomPageResponse.TasteProfile("추천 활동", "취향에 맞는 활동을 모아봤어요.", List.of("#모임")),
                new ActivitySectionResponse("customRecommended", "맞춤 추천 활동", null, List.of(card()))
        );
    }

    private ActivityDetailPageResponse detailPageResponse() {
        return new ActivityDetailPageResponse(
                1L,
                10L,
                "서울",
                "테스트 활동",
                "상세 설명",
                "https://example.com/thumb.png",
                List.of(new ActivityImageItemResponse(1L, "https://example.com/image.png", 0, true)),
                "https://example.com/apply",
                "서울",
                "운영기관",
                "010-0000-0000",
                "청년",
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(4),
                BASE_TIME,
                BASE_TIME.plusDays(3),
                3,
                0,
                ActivityType.PROGRAM,
                ActivityCategory.CRAFT,
                List.of("#공예 / 만들기", "#프로그램", "#사교"),
                SourceType.URL_MANUAL,
                null,
                ApprovalStatus.APPROVED,
                1,
                2,
                3,
                false,
                true,
                BASE_TIME.minusDays(1),
                BASE_TIME.minusDays(1)
        );
    }

    private ActivityCardResponse card() {
        return new ActivityCardResponse(
                1L,
                "테스트 활동",
                "https://example.com/thumb.png",
                3,
                10L,
                "서울",
                "서울",
                ActivityType.PROGRAM,
                ActivityCategory.CRAFT,
                List.of("#공예 / 만들기", "#프로그램"),
                0,
                1,
                2,
                3,
                false,
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(4),
                BASE_TIME,
                BASE_TIME.plusDays(3)
        );
    }
}

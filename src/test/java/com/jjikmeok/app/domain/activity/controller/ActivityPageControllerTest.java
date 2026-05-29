package com.jjikmeok.app.domain.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCardResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityHomePageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityImageItemResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivitySectionResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityShortcutResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.service.ActivityPageService;
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
class ActivityPageControllerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 28, 10, 0);

    @Mock
    private ActivityPageService activityPageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = standaloneSetup(new ActivityPageController(activityPageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getHomePage_returnsScreenSections() throws Exception {
        when(activityPageService.getHomePage(null, 5)).thenReturn(homePageResponse());

        mockMvc.perform(get("/api/v1/activities/pages/home").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("홈 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.hero.title").value("닉네임님, 오늘은 뭐 찍먹해볼까요?"))
                .andExpect(jsonPath("$.data.shortcuts[0].type").value("PROGRAM"))
                .andExpect(jsonPath("$.data.recommended.activities[0].dDay").value("D-3"));

        verify(activityPageService).getHomePage(null, 5);
    }

    @Test
    void getCategoryPage_passesFilters() throws Exception {
        when(activityPageService.getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10))
                .thenReturn(categoryPageResponse());

        mockMvc.perform(get("/api/v1/activities/pages/category")
                        .param("type", "PROGRAM")
                        .param("category", "CRAFT")
                        .param("sort", "deadline")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.selectedType").value("PROGRAM"))
                .andExpect(jsonPath("$.data.activities[0].categoryLabel").value("공예/만들기"));

        verify(activityPageService).getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10);
    }

    @Test
    void getCustomPage_returnsTasteProfile() throws Exception {
        when(activityPageService.getCustomPage(null, null)).thenReturn(customPageResponse());

        mockMvc.perform(get("/api/v1/activities/pages/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("맞춤 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.tasteProfile.title").value("우선 한 입만 먹어보는 형"));

        verify(activityPageService).getCustomPage(null, null);
    }

    @Test
    void getDetailPage_returnsDisplayFields() throws Exception {
        when(activityPageService.getDetailPage(null, 1L)).thenReturn(detailPageResponse());

        mockMvc.perform(get("/api/v1/activities/pages/detail/{activityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상세 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.organizer").value("운영기관"))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.data.priceLabel").value("무료"));

        verify(activityPageService).getDetailPage(null, 1L);
    }

    private ActivityHomePageResponse homePageResponse() {
        return new ActivityHomePageResponse(
                "닉네임",
                new ActivityHomePageResponse.Hero("닉네임님, 오늘은 뭐 찍먹해볼까요?", "가볍게 둘러보세요.", "나만의 경험 탐색하기", "/activities/pages/custom"),
                List.of(new ActivityShortcutResponse(ActivityType.PROGRAM, "프로그램", "palette", "/activities/pages/category?type=PROGRAM")),
                new ActivitySectionResponse("recommended", "닉네임님에게 추천해요!", null, List.of(card())),
                new ActivitySectionResponse("closingSoon", "인기 마감 임박", null, List.of(card()))
        );
    }

    private ActivityCategoryPageResponse categoryPageResponse() {
        return new ActivityCategoryPageResponse(
                "프로그램",
                ActivityType.PROGRAM,
                ActivityCategory.CRAFT,
                "deadline",
                1L,
                List.of(new ActivityFilterOptionResponse("PROGRAM", "프로그램", true)),
                List.of(new ActivityFilterOptionResponse("CRAFT", "공예/만들기", true)),
                List.of(new ActivityFilterOptionResponse("deadline", "마감임박순", true)),
                List.of(card())
        );
    }

    private ActivityCustomPageResponse customPageResponse() {
        return new ActivityCustomPageResponse(
                "닉네임",
                new ActivityCustomPageResponse.TasteProfile("우선 한 입만 먹어보는 형", "취향 적합도가 높은 활동이에요!", List.of("#입문")),
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
                "2026.06.01",
                "2026.05.28 ~ 2026.05.31",
                "D-3",
                3L,
                "3일 남음",
                0,
                "무료",
                ActivityType.PROGRAM,
                "프로그램",
                ActivityCategory.CRAFT,
                "공예/만들기",
                List.of("#공예/만들기"),
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
                "D-3",
                3L,
                "3일 남음",
                10L,
                "서울",
                "서울",
                ActivityType.PROGRAM,
                "프로그램",
                ActivityCategory.CRAFT,
                "공예/만들기",
                List.of("#공예/만들기", "#프로그램"),
                0,
                "무료",
                1,
                2,
                3,
                false,
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(3)
        );
    }
}

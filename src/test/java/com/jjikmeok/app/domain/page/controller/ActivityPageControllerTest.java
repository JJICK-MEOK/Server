package com.jjikmeok.app.domain.page.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.CategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.CustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.HomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityImageItemResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityListItemResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityShortcutResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
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
class ActivityPageControllerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 28, 10, 0);

    @Mock
    private PageService activityPageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = standaloneSetup(new PageController(activityPageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getHomePage_returnsScreenSections() throws Exception {
        when(activityPageService.getHomePage(null, null)).thenReturn(homePageResponse());

        mockMvc.perform(get("/api/v1/pages/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("홈 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.banner.title").value("확신이 없어도 괜찮아요 일단 찍먹 해보세요!"))
                .andExpect(jsonPath("$.data.banner.subtitle").value("다양한 활동을 부담 없이 탐색해보세요"))
                .andExpect(jsonPath("$.data.banner.currentIndex").value(1))
                .andExpect(jsonPath("$.data.shortcuts[0].type").value("PROGRAM"))
                .andExpect(jsonPath("$.data.shortcuts[0].icon").value("/images/icons/activity-program.png"))
                .andExpect(jsonPath("$.data.recommended.activities[0].dDay").value("D-3"))
                .andExpect(jsonPath("$.data.closingSoon.theme").value("dark"));

        verify(activityPageService).getHomePage(null, null);
    }

    @Test
    void getCategoryPage_passesFilters() throws Exception {
        when(activityPageService.getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10))
                .thenReturn(categoryPageResponse());

        mockMvc.perform(get("/api/v1/pages/category")
                        .param("type", "PROGRAM")
                        .param("category", "CRAFT")
                        .param("sort", "deadline")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.selectedType").value("PROGRAM"))
                .andExpect(jsonPath("$.data.activities[0].title").value("테스트 활동"))
                .andExpect(jsonPath("$.data.activities[0].dDay").value("D-3"));

        verify(activityPageService).getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10);
    }

    @Test
    void getCustomPage_returnsTasteProfile() throws Exception {
        when(activityPageService.getCustomPage(null, null)).thenReturn(customPageResponse());

        mockMvc.perform(get("/api/v1/pages/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("맞춤 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.tasteProfile.title").value("우선 한 입만 먹어보는 형"));

        verify(activityPageService).getCustomPage(null, null);
    }

    @Test
    void getDetailPage_returnsDisplayFields() throws Exception {
        when(activityPageService.getDetailPage(null, 1L)).thenReturn(detailPageResponse());

        mockMvc.perform(get("/api/v1/pages/detail/{activityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상세 화면 활동 조회 성공"))
                .andExpect(jsonPath("$.data.organizer").value("운영기관"))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.data.priceLabel").value("무료"));

        verify(activityPageService).getDetailPage(null, 1L);
    }

    private HomePageResponse homePageResponse() {
        return new HomePageResponse(
                "닉네임",
                new HomePageResponse.Banner("확신이 없어도 괜찮아요 일단 찍먹 해보세요!", "다양한 활동을 부담 없이 탐색해보세요", "나만의 경험 탐색하기", "/pages/custom", 1, 2),
                List.of(new ActivityShortcutResponse(ActivityType.PROGRAM, "프로그램", "/images/icons/activity-program.png", "/pages/category?type=PROGRAM")),
                new HomePageResponse.RecommendedSection("닉네임 님에게 추천해요!", "/pages/custom", List.of(homeRecommendedActivity())),
                new HomePageResponse.ClosingSoonSection("인기 마감 임박", "/pages/category?sort=deadline", "dark", List.of(homeClosingSoonActivity()))
        );
    }

    private CategoryPageResponse categoryPageResponse() {
        return new CategoryPageResponse(
                "프로그램",
                ActivityType.PROGRAM,
                ActivityCategory.CRAFT,
                "deadline",
                1L,
                List.of(new ActivityFilterOptionResponse("PROGRAM", "프로그램", true)),
                List.of(new ActivityFilterOptionResponse("CRAFT", "공예/만들기", true)),
                List.of(new ActivityFilterOptionResponse("deadline", "마감임박순", true)),
                List.of(listItem())
        );
    }

    private CustomPageResponse customPageResponse() {
        return new CustomPageResponse(
                "닉네임",
                new CustomPageResponse.TasteProfile("우선 한 입만 먹어보는 형", "취향 적합도가 높은 활동이에요!", List.of("#입문")),
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

    private ActivityListItemResponse listItem() {
        return new ActivityListItemResponse(
                1L,
                "테스트 활동",
                "https://example.com/thumb.png",
                "D-3",
                1,
                2,
                List.of("#공예/만들기", "#프로그램"),
                false
        );
    }

    private HomePageResponse.RecommendedActivity homeRecommendedActivity() {
        return new HomePageResponse.RecommendedActivity(
                1L,
                "테스트 활동",
                "https://example.com/thumb.png",
                "공예/만들기",
                "D-3",
                List.of("#공예/만들기", "#프로그램"),
                false
        );
    }

    private HomePageResponse.ClosingSoonActivity homeClosingSoonActivity() {
        return new HomePageResponse.ClosingSoonActivity(
                1L,
                "테스트 활동",
                "프로그램 관련 활동",
                "https://example.com/thumb.png",
                "D-3",
                "프로그램",
                "공예/만들기",
                false
        );
    }
}

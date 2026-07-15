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
import com.jjikmeok.app.domain.page.dto.response.ActivityCurationDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFavoritePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationSectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePopularActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePopularActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ImageItemResponse;
import com.jjikmeok.app.domain.page.model.HomeCurationType;
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
        when(pageService.getHomePage(null)).thenReturn(homePageResponse());

        mockMvc.perform(get("/api/v1/pages/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.nickname").value("tester"))
                .andExpect(jsonPath("$.data.user.profileImageUrl").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.data.featured.activities.length()").value(4))
                .andExpect(jsonPath("$.data.featured.activities[0].title").value(HomeCurationType.SOLO_CULTURE.getTitle()))
                .andExpect(jsonPath("$.data.featured.activities[0].thumbnailUrl").value(HomeCurationType.SOLO_CULTURE.getThumbnailUrl()))
                .andExpect(jsonPath("$.data.featured.activities[0].hashtags.length()").value(2))
                .andExpect(jsonPath("$.data.popular.activities.length()").value(9))
                .andExpect(jsonPath("$.data.popular.activities[0].thumbnailUrl").value("https://example.com/thumb.png"))
                .andExpect(jsonPath("$.data.popular.activities[0].hashtags").doesNotExist())
                .andExpect(jsonPath("$.data.expandedRecommendation.activities.length()").value(8));

        verify(pageService).getHomePage(null);
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
                .andExpect(jsonPath("$.data.selectedType").value("PROGRAM"))
                .andExpect(jsonPath("$.data.activities[0].category").value("CRAFT"))
                .andExpect(jsonPath("$.data.activities[0].hashtags.length()").value(2));

        verify(pageService).getCategoryPage(null, ActivityType.PROGRAM, ActivityCategory.CRAFT, "deadline", 10);
    }

    @Test
    void getCustomPage_returnsTasteProfile() throws Exception {
        when(pageService.getCustomPage(null, null)).thenReturn(customPageResponse());

        mockMvc.perform(get("/api/v1/pages/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasteProfile.title").value("추천 활동"));

        verify(pageService).getCustomPage(null, null);
    }

    @Test
    void getFavoritePage_returnsFavoriteCards() throws Exception {
        when(pageService.getFavoritePage(null, "saved")).thenReturn(favoritePageResponse());

        mockMvc.perform(get("/api/v1/pages/favorites").param("sort", "saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activities[0].id").value(1L))
                .andExpect(jsonPath("$.data.activities[0].liked").value(true));

        verify(pageService).getFavoritePage(null, "saved");
    }

    @Test
    void getDetailPage_returnsDisplayFields() throws Exception {
        when(pageService.getDetailPage(null, 1L)).thenReturn(detailPageResponse());

        mockMvc.perform(get("/api/v1/pages/detail/{activityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizer").value("운영기관"))
                .andExpect(jsonPath("$.data.images[0].imageUrl").value("https://example.com/image.png"))
                .andExpect(jsonPath("$.data.hashtags.length()").value(3))
                .andExpect(jsonPath("$.data.deadline").value(3));

        verify(pageService).getDetailPage(null, 1L);
    }

    @Test
    void getHomeCurationDetailPage_returnsThemeDetails() throws Exception {
        when(pageService.getHomeCurationDetailPage(null, "SOLO_CULTURE", 0, 20)).thenReturn(homeCurationDetailResponse());

        mockMvc.perform(get("/api/v1/pages/home/curations/{curationKey}", "SOLO_CULTURE")
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(HomeCurationType.SOLO_CULTURE.getTitle()))
                .andExpect(jsonPath("$.data.activities[0].title").value("서울 도예 원데이 클래스"))
                .andExpect(jsonPath("$.data.activities[0].hashtags.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.limit").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextPage").value(1));

        verify(pageService).getHomeCurationDetailPage(null, "SOLO_CULTURE", 0, 20);
    }

    private ActivityHomePageResponse homePageResponse() {
        return new ActivityHomePageResponse(
                new ActivityHomePageResponse.UserResponse("tester", "https://example.com/profile.png"),
                new ActivityHomeCurationSectionResponse(List.of(
                        new ActivityHomeCurationCardResponse(
                                HomeCurationType.SOLO_CULTURE.getTitle(),
                                HomeCurationType.SOLO_CULTURE.getThumbnailUrl(),
                                List.of("#감성적", "#소규모")
                        ),
                        new ActivityHomeCurationCardResponse(
                                HomeCurationType.NEW_INSPIRATION.getTitle(),
                                HomeCurationType.NEW_INSPIRATION.getThumbnailUrl(),
                                List.of("#트렌디", "#창의적")
                        ),
                        new ActivityHomeCurationCardResponse(
                                HomeCurationType.WEEKEND_FUN.getTitle(),
                                HomeCurationType.WEEKEND_FUN.getThumbnailUrl(),
                                List.of("#가볍게", "#단기")
                        ),
                        new ActivityHomeCurationCardResponse(
                                HomeCurationType.HEALING.getTitle(),
                                HomeCurationType.HEALING.getThumbnailUrl(),
                                List.of("#힐링", "#휴식")
                        )
                )),
                new ActivityHomePopularActivitySectionResponse(List.of(
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false),
                        new ActivityHomePopularActivityCardResponse(1L, "서울 도예 원데이 클래스", "https://example.com/thumb.png", "프로그램", 3, false)
                )),
                new ActivityHomeActivitySectionResponse(List.of(
                        homeActivityCard(), homeActivityCard(), homeActivityCard(), homeActivityCard(),
                        homeActivityCard(), homeActivityCard(), homeActivityCard(), homeActivityCard()
                ))
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
                        new ActivityFilterOptionResponse("EVENT", "행사/강연", false),
                        new ActivityFilterOptionResponse("CLUB", "동아리", false)
                ),
                List.of(
                        new ActivityFilterOptionResponse("", "전체", false),
                        new ActivityFilterOptionResponse("SPORTS", "운동 / 액티비티", false),
                        new ActivityFilterOptionResponse("CULTURE", "문화 / 예술", false),
                        new ActivityFilterOptionResponse("CRAFT", "공예 / 만들기", true),
                        new ActivityFilterOptionResponse("COOKING", "요리 / 베이킹", false),
                        new ActivityFilterOptionResponse("PHOTO_VIDEO", "사진 / 영상", false),
                        new ActivityFilterOptionResponse("HUMANITIES", "독서 / 글", false),
                        new ActivityFilterOptionResponse("TRAVEL", "여행 / 모험", false),
                        new ActivityFilterOptionResponse("LANGUAGE", "언어 / 외국", false),
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
                new ActivityCustomPageResponse.TasteProfile("추천 활동", "취향에 맞는 활동을 모아봤어요", List.of("#몰입")),
                new ActivitySectionResponse(List.of(card()))
        );
    }

    private ActivityFavoritePageResponse favoritePageResponse() {
        return new ActivityFavoritePageResponse(List.of(favoriteCard()));
    }

    private ActivityDetailPageResponse detailPageResponse() {
        return new ActivityDetailPageResponse(
                1L,
                10L,
                "서울",
                "테스트 활동",
                "상세 설명",
                "https://example.com/thumb.png",
                List.of(new ImageItemResponse(1L, "https://example.com/image.png", 0, true)),
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
                List.of("#공예", "#프로그램", "#창작"),
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

    private ActivityCurationDetailPageResponse homeCurationDetailResponse() {
        return new ActivityCurationDetailPageResponse(
                HomeCurationType.SOLO_CULTURE.getTitle(),
                "혼자서도 부담 없이 즐길 수 있는 활동을 모아봤어요",
                List.of("#감성적", "#소규모"),
                List.of(homeActivityCard()),
                0,
                20,
                true,
                1
        );
    }

    private ActivityHomeActivityCardResponse homeActivityCard() {
        return new ActivityHomeActivityCardResponse(
                1L,
                "서울 도예 원데이 클래스",
                "https://example.com/thumb.png",
                "프로그램",
                3,
                List.of("#공예", "#프로그램"),
                false
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
                List.of("#공예", "#프로그램"),
                false,
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

    private ActivityCardResponse favoriteCard() {
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
                List.of("#공예", "#프로그램"),
                false,
                0,
                1,
                2,
                3,
                true,
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(4),
                BASE_TIME,
                BASE_TIME.plusDays(3)
        );
    }
}

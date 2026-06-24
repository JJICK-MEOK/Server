package com.jjikmeok.app.domain.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 14, 10, 0);

    @Mock
    private ActivityService activityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = standaloneSetup(new ActivityController(activityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getActivities_returnsActivityList() throws Exception {
        when(activityService.getActivities(null, null, null, null)).thenReturn(List.of(activitySummaryResponse()));

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활동 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].regionId").value(10))
                .andExpect(jsonPath("$.data[0].title").value("테스트 활동"))
                .andExpect(jsonPath("$.data[0].price").value(0));

        verify(activityService).getActivities(null, null, null, null);
    }

    @Test
    void getActivities_withRegionId_returnsFilteredActivityList() throws Exception {
        when(activityService.getActivities(10L, null, null, null)).thenReturn(List.of(activitySummaryResponse()));

        mockMvc.perform(get("/api/v1/activities").param("regionId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].regionId").value(10));

        verify(activityService).getActivities(10L, null, null, null);
    }

    @Test
    void getActivities_whenRegionNotFound_returnsNotFound() throws Exception {
        when(activityService.getActivities(999L, null, null, null)).thenThrow(new CustomException(ErrorCode.REGION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/activities").param("regionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGION_404"));
    }

    @Test
    void getActivity_returnsActivityDetail() throws Exception {
        when(activityService.getActivity(1L)).thenReturn(activityResponse());

        mockMvc.perform(get("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활동 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.regionId").value(10))
                .andExpect(jsonPath("$.data.title").value("테스트 활동"))
                .andExpect(jsonPath("$.data.price").value(0));

        verify(activityService).getActivity(1L);
    }

    @Test
    void getActivity_whenActivityNotFound_returnsNotFound() throws Exception {
        when(activityService.getActivity(1L)).thenThrow(new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVITY_404"));
    }

    @Test
    void createActivity_returnsCreated() throws Exception {
        ActivityRequest request = activityRequest();
        when(activityService.createActivity(request)).thenReturn(activityResponse());

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활동 생성 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.price").value(0));

        verify(activityService).createActivity(request);
    }

    @Test
    void createActivity_whenRegionNotFound_returnsNotFound() throws Exception {
        ActivityRequest request = activityRequest();
        when(activityService.createActivity(request)).thenThrow(new CustomException(ErrorCode.REGION_NOT_FOUND));

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGION_404"));
    }

    @Test
    void createActivity_whenUriInvalid_returnsBadRequest() throws Exception {
        ActivityRequest request = activityRequest();
        when(activityService.createActivity(request)).thenThrow(new CustomException(ErrorCode.ACTIVITY_INVALID_URI));

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTIVITY_400_URI"));
    }

    @Test
    void updateActivity_returnsOk() throws Exception {
        ActivityRequest request = activityRequest();
        when(activityService.updateActivity(1L, request)).thenReturn(activityResponse());

        mockMvc.perform(put("/api/v1/activities/{activityId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활동 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(activityService).updateActivity(1L, request);
    }

    @Test
    void updateActivity_whenActivityNotFound_returnsNotFound() throws Exception {
        ActivityRequest request = activityRequest();
        when(activityService.updateActivity(1L, request)).thenThrow(new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(put("/api/v1/activities/{activityId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVITY_404"));
    }

    @Test
    void deleteActivity_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(activityService).deleteActivity(1L);
    }

    @Test
    void deleteActivity_whenActivityNotFound_returnsNotFound() throws Exception {
        doThrow(new CustomException(ErrorCode.ACTIVITY_NOT_FOUND)).when(activityService).deleteActivity(1L);

        mockMvc.perform(delete("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVITY_404"));
    }

    private ActivityRequest activityRequest() {
        return new ActivityRequest(
                10L,
                "테스트 활동",
                "활동 상세 설명",
                "https://example.com/thumb.png",
                "https://example.com/apply",
                "서울마포도서관",
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                BASE_TIME,
                BASE_TIME.plusDays(7),
                0,
                ActivityType.ONE_DAY,
                ActivityCategory.CRAFT,
                SourceType.URL_MANUAL,
                null,
                ApprovalStatus.PENDING,
                true
        );
    }

    private ActivitySummaryResponse activitySummaryResponse() {
        return new ActivitySummaryResponse(
                1L,
                10L,
                "서울",
                "테스트 활동",
                "https://example.com/thumb.png",
                "서울마포도서관",
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                BASE_TIME.plusDays(7),
                ActivityType.ONE_DAY,
                ActivityCategory.CRAFT,
                List.of(),
                0,
                SourceType.URL_MANUAL,
                ApprovalStatus.PENDING,
                3,
                2,
                1,
                false,
                BASE_TIME.minusDays(1)
        );
    }

    private ActivityDetailResponse activityResponse() {
        return new ActivityDetailResponse(
                1L,
                10L,
                "서울",
                "테스트 활동",
                "활동 상세 설명",
                "https://example.com/thumb.png",
                "https://example.com/apply",
                "서울마포도서관",
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                BASE_TIME,
                BASE_TIME.plusDays(7),
                0,
                ActivityType.ONE_DAY,
                ActivityCategory.CRAFT,
                List.of(),
                SourceType.URL_MANUAL,
                null,
                ApprovalStatus.PENDING,
                3,
                2,
                1,
                true,
                BASE_TIME.minusDays(1),
                BASE_TIME.minusDays(1)
        );
    }
}

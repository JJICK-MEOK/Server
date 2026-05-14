package com.jjikmeok.app.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.controller.ActivityController;
import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.enums.AgeRange;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.domain.region.controller.RegionController;
import com.jjikmeok.app.domain.region.dto.request.RegionRequest;
import com.jjikmeok.app.domain.region.dto.response.RegionResponse;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.service.RegionService;
import com.jjikmeok.app.global.common.exception.GlobalExceptionHandler;
import com.jjikmeok.app.global.config.SecurityConfig;
import com.jjikmeok.app.global.security.filter.JwtAuthenticationFilter;
import com.jjikmeok.app.global.security.handler.RestAccessDeniedHandler;
import com.jjikmeok.app.global.security.handler.RestAuthenticationEntryPoint;
import com.jjikmeok.app.global.security.handler.SecurityErrorResponseWriter;
import com.jjikmeok.app.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ActivityController.class, RegionController.class})
@Import({
        SecurityConfig.class,
        RestAccessDeniedHandler.class,
        RestAuthenticationEntryPoint.class,
        SecurityErrorResponseWriter.class,
        GlobalExceptionHandler.class,
        ActivityRegionAdminApiSecurityTest.PassthroughJwtFilterConfig.class
})
class ActivityRegionAdminApiSecurityTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 14, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private RegionService regionService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(activityService, regionService);
    }

    @Test
    void createActivity_withoutLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        verifyNoInteractions(activityService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createActivity_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(activityService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createActivity_withAdminRole_returnsCreated() throws Exception {
        when(activityService.createActivity(any(ActivityRequest.class))).thenReturn(activityResponse());

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(activityService).createActivity(any(ActivityRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void updateActivity_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/activities/{activityId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(activityService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateActivity_withAdminRole_returnsOk() throws Exception {
        when(activityService.updateActivity(any(Long.class), any(ActivityRequest.class))).thenReturn(activityResponse());

        mockMvc.perform(put("/api/v1/activities/{activityId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(activityService).updateActivity(any(Long.class), any(ActivityRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void deleteActivity_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(activityService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteActivity_withAdminRole_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/activities/{activityId}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(activityService).deleteActivity(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createRegion_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regionRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(regionService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createRegion_withAdminRole_returnsCreated() throws Exception {
        when(regionService.createRegion(any(RegionRequest.class))).thenReturn(regionResponse());

        mockMvc.perform(post("/api/v1/regions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regionRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(regionService).createRegion(any(RegionRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void updateRegion_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/regions/{regionId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regionRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(regionService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateRegion_withAdminRole_returnsOk() throws Exception {
        when(regionService.updateRegion(any(Long.class), any(RegionRequest.class))).thenReturn(regionResponse());

        mockMvc.perform(put("/api/v1/regions/{regionId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regionRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(regionService).updateRegion(any(Long.class), any(RegionRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void deleteRegion_withUserRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/regions/{regionId}", 10L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        verifyNoInteractions(regionService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteRegion_withAdminRole_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/regions/{regionId}", 10L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(regionService).deleteRegion(10L);
    }

    private ActivityRequest activityRequest() {
        return new ActivityRequest(
                10L,
                "테스트 활동",
                "https://example.com/thumb.png",
                "https://example.com/apply",
                "서울마포도서관",
                BASE_TIME,
                BASE_TIME.plusDays(7),
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                AgeRange.ANYONE,
                0,
                "활동 상세 설명",
                true
        );
    }

    private ActivityDetailResponse activityResponse() {
        return new ActivityDetailResponse(
                1L,
                10L,
                "서울",
                "테스트 활동",
                "https://example.com/thumb.png",
                "https://example.com/apply",
                "서울마포도서관",
                BASE_TIME,
                BASE_TIME.plusDays(7),
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                AgeRange.ANYONE,
                0,
                "활동 상세 설명",
                0,
                0,
                0,
                true,
                BASE_TIME.minusDays(1),
                BASE_TIME.minusDays(1)
        );
    }

    private RegionRequest regionRequest() {
        return new RegionRequest(1L, "강남구", RegionDepth.DISTRICT);
    }

    private RegionResponse regionResponse() {
        return new RegionResponse(10L, 1L, "강남구", RegionDepth.DISTRICT);
    }

    @TestConfiguration
    static class PassthroughJwtFilterConfig {

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(
                    Mockito.mock(JwtTokenProvider.class),
                    Mockito.mock(SecurityErrorResponseWriter.class)
            ) {
                @Override
                protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain
                ) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}

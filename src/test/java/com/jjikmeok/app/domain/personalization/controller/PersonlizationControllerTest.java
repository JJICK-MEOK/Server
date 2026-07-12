package com.jjikmeok.app.domain.personalization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.service.PersonlizationService;
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
class PersonlizationControllerTest {

    @Mock
    private PersonlizationService personlizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = standaloneSetup(new PersonlizationController(personlizationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getPersonlizedActivity_acceptsMisspelledBasePathUsedByClient() throws Exception {
        when(personlizationService.getRecommendedActivities(null)).thenReturn(List.of(activityRecommendation()));

        mockMvc.perform(get("/api/v1/personlization/users/me/personlization-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].personalizationScore").value(90))
                .andExpect(jsonPath("$.data[0].tags[0]").value("healing"));

        verify(personlizationService).getRecommendedActivities(null);
    }

    @Test
    void getPersonlizedActivity_acceptsCorrectedActivityPath() throws Exception {
        when(personlizationService.getRecommendedActivities(null)).thenReturn(List.of(activityRecommendation()));

        mockMvc.perform(get("/api/v1/personalization/users/me/personalization-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].personalizationScore").value(90));

        verify(personlizationService).getRecommendedActivities(null);
    }

    @Test
    void getPersonalizedContent_acceptsMisspelledBasePath() throws Exception {
        when(personlizationService.findBestType(null)).thenReturn(new PersonalizationResponse("type", List.of("tag")));

        mockMvc.perform(get("/api/v1/personlization/users/me/best-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.bestType").value("type"));

        verify(personlizationService).findBestType(null);
    }

    private ActivityRecommendationResponse activityRecommendation() {
        return new ActivityRecommendationResponse(
                1L,
                "https://example.com/thumb.png",
                "Activity",
                LocalDateTime.of(2026, 7, 10, 0, 0),
                10L,
                90,
                new String[]{"healing", "rest"}
        );
    }
}

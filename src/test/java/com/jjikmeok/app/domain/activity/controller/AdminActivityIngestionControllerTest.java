package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.enums.PublicActivitySourceType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.activity.service.AdminActivityIngestionService;
import com.jjikmeok.app.global.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AdminActivityIngestionControllerTest {

    @Mock
    private AdminActivityIngestionService adminActivityIngestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AdminActivityIngestionController(adminActivityIngestionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void syncPublicSource_returnsSyncCounts() throws Exception {
        ActivitySyncResponse response = new ActivitySyncResponse(SourceType.KOPIS, 3, 24, 7);
        when(adminActivityIngestionService.syncPublicSource(PublicActivitySourceType.KOPIS, 2)).thenReturn(response);

        mockMvc.perform(post("/api/admin/activities/sources/public/{sourceType}/sync", "KOPIS")
                        .param("maxPages", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.sourceType").value("KOPIS"))
                .andExpect(jsonPath("$.data.rawSavedCount").value(3))
                .andExpect(jsonPath("$.data.activitySavedCount").value(24))
                .andExpect(jsonPath("$.data.duplicatedCount").value(7));

        verify(adminActivityIngestionService).syncPublicSource(PublicActivitySourceType.KOPIS, 2);
    }

    @Test
    void syncPublicSource_rejectsNonPublicSourceType() throws Exception {
        mockMvc.perform(post("/api/admin/activities/sources/public/{sourceType}/sync", "DISCOVERY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_PARAMETER"));

        verifyNoInteractions(adminActivityIngestionService);
    }
}

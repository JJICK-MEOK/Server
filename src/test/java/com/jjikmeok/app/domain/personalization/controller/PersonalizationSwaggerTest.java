package com.jjikmeok.app.domain.personalization.controller;

import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jjikmeok-swagger;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-mail-password",
        "spring.ai.openai.api-key=test-openai-key",
        "storage.type=local",
        "oauth2.kakao.client-id=test-kakao-client-id",
        "oauth2.google.client-id=test-google-client-id",
        "oauth2.google.client-secret=test-google-client-secret",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
class PersonalizationSwaggerTest {

    @MockitoBean
    AiActivityParser aiActivityParser;

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiDocsIncludePersonalizationScore() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("personalizationScore")));
    }
}

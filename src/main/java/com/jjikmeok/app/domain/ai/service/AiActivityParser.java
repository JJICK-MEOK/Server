package com.jjikmeok.app.domain.ai.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.ai.dto.ExtractedActivityDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiActivityParser {

    private static final int MAX_CONTEXT_CHARS = 18_000;

    private final ChatClient chatClient;
    private final BeanOutputConverter<ExtractedActivityDto> outputConverter;

    public AiActivityParser(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ExtractedActivityDto.class);
    }

    public ExtractedActivityDto parseFallback(String coreContext, SourceType sourceType) {
        return parse(coreContext, sourceType, "fallback");
    }

    public ExtractedActivityDto parseDiscovery(String coreContext) {
        return parse(coreContext, null, "discovery");
    }

    private ExtractedActivityDto parse(String coreContext, SourceType sourceType, String mode) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt(sourceType, mode))
                    .user(u -> u.text("""
                            [분석 대상 텍스트]
                            {text}

                            [출력 스키마]
                            {format}
                            """)
                            .param("text", compact(coreContext))
                            .param("format", outputConverter.getFormat()))
                    .call()
                    .entity(outputConverter);
        } catch (Exception e) {
            log.warn("[AI] parse failed. mode={}, sourceType={}, reason={}", mode, sourceType, e.getMessage());
            return empty();
        }
    }

    private ExtractedActivityDto empty() {
        return new ExtractedActivityDto(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );
    }

    private String systemPrompt(SourceType sourceType, String mode) {
        return """
                You extract missing activity fields.
                Return JSON only.
                Do not invent values. Use null when unsure.
                Use only the allowed enum names for these fields:
                - category: SPORTS, CULTURE, CRAFT, COOKING, PHOTO_VIDEO, HUMANITIES, TRAVEL, LANGUAGE, VOLUNTEER, CAREER
                - activityType: PROGRAM, ONE_DAY, EVENT, CLUB
                - moodTag1/moodTag2: CALM, HEALING, LIVELY, EMOTIONAL, CREATIVE, TRENDY
                - intensity: BEGINNER, LIGHT, IMMERSIVE, CHALLENGE
                - purpose: REST, HOBBY, LEARNING, GROWTH
                - duration: SHORT_TERM, ONE_MONTH, SIX_MONTHS, OVER_ONE_YEAR
                - groupSize: SMALL, LARGE

                Fields:
                title, address, category, activityType, moodTag1, moodTag2, intensity, purpose, duration, groupSize,
                recruitStartAt, recruitEndAt, startAt, endAt, price, description, target, contactInfo, organizer

                """ + sourcePrompt(sourceType, mode);
    }

    private String sourcePrompt(SourceType sourceType, String mode) {
        if (sourceType == null && !"fallback".equals(mode)) {
            return "";
        }

        if (sourceType == null) {
            return "";
        }

        return switch (sourceType) {
            case KOPIS -> """
                    [KOPIS]
                    - Use performance period for startAt/endAt.
                    - organizer is the organizer or host.
                    - contactInfo is inquiry phone or email.
                    - address is venue/location if present.
                    """;
            case EXHIBITION -> """
                    [EXHIBITION]
                    - Use PERIOD / EVENT_PERIOD for startAt/endAt.
                    - contactInfo from CONTACT_POINT.
                    - organizer from CONTRIBUTOR.
                    - address from venue/location if present.
                    """;
            case SEOUL_CULTURE -> """
                    [SEOUL_CULTURE]
                    - Use DATE / END_DATE for startAt/endAt.
                    - organizer from ORG_NAME.
                    - contactInfo from INQUIRY.
                    - target from USE_TRGT when available.
                    """;
            case SEOUL_RESERVATION -> """
                    [SEOUL_RESERVATION]
                    - Use RCPTBGNDT / RCPTENDDT for recruitStartAt/recruitEndAt.
                    - Use SVCOPNBGNDT / SVCOPNENDDT for startAt/endAt.
                    - contactInfo from TELNO.
                    - target from USETGTINFO or USE_TRGT.
                    """;
            default -> "";
        };
    }

    private String compact(String value) {
        if (value == null || value.length() <= MAX_CONTEXT_CHARS) {
            return value;
        }
        int head = MAX_CONTEXT_CHARS / 2;
        int tail = MAX_CONTEXT_CHARS - head;
        return value.substring(0, head) + "\n...[truncated]...\n" + value.substring(value.length() - tail);
    }
}

package com.jjikmeok.app.domain.personalization.dto;

import java.util.List;

public record PersonalizationResponse(
        String bestType,
        List<String> userTags
) {
}

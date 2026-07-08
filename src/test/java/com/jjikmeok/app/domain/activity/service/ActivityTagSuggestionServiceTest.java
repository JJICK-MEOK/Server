package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityTagSuggestionServiceTest {

    @Mock
    private ActivitySyncUtils utils;

    private ActivityTagSuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        suggestionService = new ActivityTagSuggestionService(utils);
    }

    @Test
    void suggest_returnsFiveTagsIncludingOneMood() {
        when(utils.cleanText("테스트 활동")).thenReturn("테스트 활동");

        var tags = suggestionService.suggest(
                "테스트 활동",
                ActivityCategory.CULTURE,
                0,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 3, 0, 0)
        );

        assertThat(tags).hasSize(5);
        assertThat(tags).filteredOn(tag -> tag.getGroup() == TagGroupType.MOOD).hasSize(1);
    }
}

package com.jjikmeok.app.domain.activity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityTaxonomyTest {

    @Test
    void activityTypes_includeAllDefinedValues() {
        assertThat(ActivityType.values()).hasSize(4);
        assertThat(ActivityType.values())
                .extracting(ActivityType::getLabel)
                .allMatch(label -> label != null && !label.isBlank());
    }

    @Test
    void activityCategories_includeAllDefinedValues() {
        assertThat(ActivityCategory.values()).hasSize(10);
        assertThat(ActivityCategory.values())
                .extracting(ActivityCategory::getLabel)
                .allMatch(label -> label != null && !label.isBlank());
    }

    @Test
    void preferenceTags_areRenderedAsHashtags() {
        assertThat(PreferenceTag.values()).isNotEmpty();
        assertThat(PreferenceTag.values())
                .extracting(PreferenceTag::getHashtag)
                .allMatch(hashtag -> hashtag != null && hashtag.startsWith("#"));
    }
}

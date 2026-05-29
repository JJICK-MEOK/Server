package com.jjikmeok.app.domain.tag.entity;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class TagCatalog {

    private TagCatalog() {
    }

    public static boolean contains(TagType type, String name) {
        if (type == null || name == null) {
            return false;
        }

        return switch (type) {
            case ACTIVITY_CATEGORY -> ActivityType.containsLabel(name);
            case TOPIC_CATEGORY -> ActivityCategory.containsLabel(name);
            case PREFERENCE_TAG -> PreferenceTag.containsLabel(name);
        };
    }

    public static List<Entry> entries() {
        return Stream.of(
                        Arrays.stream(ActivityCategory.values())
                                .map(category -> new Entry(category.getLabel(), TagType.TOPIC_CATEGORY)),
                        Arrays.stream(ActivityType.values())
                                .map(type -> new Entry(type.getLabel(), TagType.ACTIVITY_CATEGORY)),
                        Arrays.stream(PreferenceTag.values())
                                .map(tag -> new Entry(tag.getLabel(), TagType.PREFERENCE_TAG))
                )
                .flatMap(stream -> stream)
                .toList();
    }

    public record Entry(String name, TagType type) {
    }
}

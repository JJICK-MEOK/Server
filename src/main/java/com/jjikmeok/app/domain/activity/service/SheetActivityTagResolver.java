package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SheetActivityTagResolver {

    private final TagRepository tagRepository;

    public List<Long> resolveTagIds(DiscoverySheetRowDto row) {
        if (row == null
                || row.moodTag1() == null
                || row.moodTag2() == null
                || row.intensity() == null
                || row.purpose() == null
                || row.duration() == null
                || row.groupSize() == null) {
            throw new IllegalArgumentException("분위기 2개, 강도, 목적, 기간, 규모 태그를 모두 입력해야 합니다.");
        }

        List<PreferenceTag> requested = List.of(
                preferenceTag(row.moodTag1()),
                preferenceTag(row.moodTag2()),
                preferenceTag(row.intensity()),
                preferenceTag(row.purpose()),
                preferenceTag(row.duration()),
                preferenceTag(row.groupSize())
        );
        if (new LinkedHashSet<>(requested).size() != 6) {
            throw new IllegalArgumentException("시트의 6개 태그는 서로 달라야 합니다.");
        }

        List<Tag> availableTags = tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG);
        return requested.stream()
                .map(preferenceTag -> resolveTagId(preferenceTag, availableTags))
                .toList();
    }

    private PreferenceTag preferenceTag(Enum<?> sheetTag) {
        try {
            return PreferenceTag.valueOf(sheetTag.name().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.TAG_NOT_FOUND);
        }
    }

    private Long resolveTagId(PreferenceTag preferenceTag, List<Tag> availableTags) {
        String expectedName = normalizeTagName(preferenceTag.getHashtag());
        return availableTags.stream()
                .filter(tag -> tag.getTagGroupType() == null || tag.getTagGroupType() == preferenceTag.getGroup())
                .filter(tag -> expectedName.equals(normalizeTagName(tag.getName())))
                .map(Tag::getId)
                .filter(id -> id != null)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
    }

    private String normalizeTagName(String value) {
        return value == null ? "" : value.replace("#", "").replaceAll("\\s+", "").trim();
    }
}

package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SheetActivityTagResolverTest {

    @Test
    void resolveTagIds_mapsSixSheetTagsToExistingTagIds() {
        TagRepository tagRepository = mock(TagRepository.class);
        SheetActivityTagResolver resolver = new SheetActivityTagResolver(tagRepository);
        when(tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG)).thenReturn(List.of(
                tag(1L, "#편안한", TagGroupType.MOOD),
                tag(2L, "#힐링", TagGroupType.MOOD),
                tag(8L, "#가볍게", TagGroupType.INTENSITY),
                tag(12L, "#취미", TagGroupType.PURPOSE),
                tag(18L, "#1년이상", TagGroupType.DURATION),
                tag(19L, "#소규모", TagGroupType.SIZE)
        ));

        List<Long> tagIds = resolver.resolveTagIds(rowWithTags(
                "CALM", "HEALING", "LIGHT", "HOBBY", "OVER_ONE_YEAR", "SMALL"
        ));

        assertThat(tagIds).containsExactly(1L, 2L, 8L, 12L, 18L, 19L);
    }

    @Test
    void resolveTagIds_whenAnySheetTagIsMissing_rejectsPublish() {
        SheetActivityTagResolver resolver = new SheetActivityTagResolver(mock(TagRepository.class));

        assertThatThrownBy(() -> resolver.resolveTagIds(rowWithTags(
                "CALM", null, "LIGHT", "HOBBY", "ONE_MONTH", "SMALL"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("태그를 모두 입력");
    }

    private DiscoverySheetRowDto rowWithTags(
            String mood1,
            String mood2,
            String intensity,
            String purpose,
            String duration,
            String size
    ) {
        List<Object> values = new ArrayList<>(Collections.nCopies(29, null));
        values.set(0, 217);
        values.set(4, "발행대기");
        values.set(5, "CULTURE");
        values.set(6, "EVENT");
        values.set(21, mood1);
        values.set(22, mood2);
        values.set(23, intensity);
        values.set(24, purpose);
        values.set(25, duration);
        values.set(26, size);
        values.set(27, "DISCOVERY");
        return DiscoverySheetRowDto.fromSheetRow(218, values);
    }

    private Tag tag(Long id, String name, TagGroupType groupType) {
        Tag tag = Tag.create(name, TagType.PREFERENCE_TAG, groupType);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }
}

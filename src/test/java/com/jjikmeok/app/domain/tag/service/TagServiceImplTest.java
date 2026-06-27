package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.activity.enums.PreferenceTagGroup;
import com.jjikmeok.app.domain.tag.dto.request.TagRequest;
import com.jjikmeok.app.domain.tag.dto.response.PreferenceTagGroupResponse;
import com.jjikmeok.app.domain.tag.dto.response.TagResponse;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagServiceImpl(tagRepository);
    }

    @Test
    void getTags_withoutType_returnsAllTagsInRepositoryOrder() {
        when(tagRepository.findAllByOrderByTypeAscNameAsc()).thenReturn(List.of(
                tag(1L, "프로그램", TagType.ACTIVITY_CATEGORY),
                tag(2L, "차분", TagType.PREFERENCE_TAG)
        ));

        List<TagResponse> responses = tagService.getTags(null);

        assertThat(responses).extracting(TagResponse::id).containsExactly(1L, 2L);
        verify(tagRepository).findAllByOrderByTypeAscNameAsc();
    }

    @Test
    void getTags_withType_returnsFilteredTags() {
        when(tagRepository.findAllByTypeOrderByNameAsc(TagType.TOPIC_CATEGORY)).thenReturn(List.of(
                tag(3L, "문화/공연/축제", TagType.TOPIC_CATEGORY)
        ));

        List<TagResponse> responses = tagService.getTags(TagType.TOPIC_CATEGORY);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().type()).isEqualTo(TagType.TOPIC_CATEGORY);
        verify(tagRepository).findAllByTypeOrderByNameAsc(TagType.TOPIC_CATEGORY);
    }

    @Test
    void getPreferenceTagGroups_returnsPreferenceTagsGroupedByTaxonomy() {
        when(tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG)).thenReturn(List.of(
                tag(1L, "편안한", TagType.PREFERENCE_TAG),
                tag(2L, "활기찬", TagType.PREFERENCE_TAG),
                tag(3L, "사교", TagType.PREFERENCE_TAG),
                tag(4L, "단기", TagType.PREFERENCE_TAG),
                tag(5L, "입문", TagType.PREFERENCE_TAG),
                tag(6L, "소규모", TagType.PREFERENCE_TAG)
        ));

        List<PreferenceTagGroupResponse> responses = tagService.getPreferenceTagGroups();

        assertThat(responses).extracting(PreferenceTagGroupResponse::group)
                .containsExactly(
                        PreferenceTagGroup.MOOD,
                        PreferenceTagGroup.INTENSITY,
                        PreferenceTagGroup.PURPOSE,
                        PreferenceTagGroup.DURATION,
                        PreferenceTagGroup.SIZE
        );
        assertThat(responses.getFirst().label()).isEqualTo("분위기 태그");
        assertThat(responses.getFirst().tags()).extracting(TagResponse::name).containsExactly("편안한", "활기찬");
        assertThat(responses.get(1).tags()).extracting(TagResponse::name).containsExactly("입문");
        assertThat(responses.get(2).tags()).extracting(TagResponse::name).containsExactly("사교");
        assertThat(responses.get(3).tags()).extracting(TagResponse::name).containsExactly("단기");
        assertThat(responses.get(4).tags()).extracting(TagResponse::name).containsExactly("소규모");
    }

    @Test
    void getTag_whenNotFound_throwsTagNotFound() {
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> tagService.getTag(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND);
    }

    @Test
    void createTag_trimsNameBeforeSaving() {
        TagRequest request = new TagRequest(" 프로그램 ", TagType.ACTIVITY_CATEGORY);
        when(tagRepository.existsByNameAndType("프로그램", TagType.ACTIVITY_CATEGORY)).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            setId(saved, 1L);
            return saved;
        });

        TagResponse response = tagService.createTag(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("프로그램");
    }

    @Test
    void createTag_whenNameAndTypeDuplicate_throwsDuplicateName() {
        TagRequest request = new TagRequest("프로그램", TagType.ACTIVITY_CATEGORY);
        when(tagRepository.existsByNameAndType("프로그램", TagType.ACTIVITY_CATEGORY)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () -> tagService.createTag(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_DUPLICATE_NAME);
        verify(tagRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTag_whenSaveConflicts_throwsDuplicateName() {
        TagRequest request = new TagRequest("프로그램", TagType.ACTIVITY_CATEGORY);
        when(tagRepository.existsByNameAndType("프로그램", TagType.ACTIVITY_CATEGORY)).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        CustomException exception = assertThrows(CustomException.class, () -> tagService.createTag(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_DUPLICATE_NAME);
    }

    @Test
    void updateTag_updatesNameAndType() {
        Tag tag = tag(1L, "프로그램", TagType.ACTIVITY_CATEGORY);
        TagRequest request = new TagRequest(" 운동 / 액티비티 ", TagType.TOPIC_CATEGORY);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameAndTypeAndIdNot("운동 / 액티비티", TagType.TOPIC_CATEGORY, 1L)).thenReturn(false);
        when(tagRepository.saveAndFlush(tag)).thenReturn(tag);

        TagResponse response = tagService.updateTag(1L, request);

        assertThat(response.name()).isEqualTo("운동 / 액티비티");
        assertThat(response.type()).isEqualTo(TagType.TOPIC_CATEGORY);
    }

    @Test
    void updateTag_whenFlushConflicts_throwsDuplicateName() {
        Tag tag = tag(1L, "프로그램", TagType.ACTIVITY_CATEGORY);
        TagRequest request = new TagRequest(" 운동 / 액티비티 ", TagType.TOPIC_CATEGORY);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameAndTypeAndIdNot("운동 / 액티비티", TagType.TOPIC_CATEGORY, 1L)).thenReturn(false);
        when(tagRepository.saveAndFlush(tag)).thenThrow(new DataIntegrityViolationException("duplicate"));

        CustomException exception = assertThrows(CustomException.class, () -> tagService.updateTag(1L, request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_DUPLICATE_NAME);
    }

    @Test
    void deleteTag_whenRepositoryRejectsDelete_throwsTagInUse() {
        Tag tag = tag(1L, "프로그램", TagType.ACTIVITY_CATEGORY);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        doThrow(new DataIntegrityViolationException("fk")).when(tagRepository).flush();

        CustomException exception = assertThrows(CustomException.class, () -> tagService.deleteTag(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_IN_USE);
        verify(tagRepository).delete(tag);
    }

    private Tag tag(Long id, String name, TagType type) {
        Tag tag = Tag.create(name, type);
        setId(tag, id);
        return tag;
    }

    private void setId(Tag tag, Long id) {
        ReflectionTestUtils.setField(tag, "id", id);
    }
}

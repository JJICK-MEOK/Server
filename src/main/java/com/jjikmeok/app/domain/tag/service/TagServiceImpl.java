package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.PreferenceTagGroup;
import com.jjikmeok.app.domain.tag.converter.TagConverter;
import com.jjikmeok.app.domain.tag.dto.request.TagRequest;
import com.jjikmeok.app.domain.tag.dto.response.PreferenceTagGroupResponse;
import com.jjikmeok.app.domain.tag.dto.response.TagResponse;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagCatalog;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public List<TagResponse> getTags(TagType type) {
        List<Tag> tags = type == null
                ? tagRepository.findAllByOrderByTypeAscNameAsc()
                : tagRepository.findAllByTypeOrderByNameAsc(type);

        return tags.stream()
                .map(TagConverter::toResponse)
                .toList();
    }

    @Override
    public List<PreferenceTagGroupResponse> getPreferenceTagGroups() {
        Map<String, Tag> tagsByName = tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity(), (left, right) -> left));
        Map<PreferenceTagGroup, List<TagResponse>> grouped = new EnumMap<>(PreferenceTagGroup.class);

        Arrays.stream(PreferenceTag.values())
                .filter(tag -> tagsByName.containsKey(tag.getLabel()))
                .forEach(tag -> grouped.computeIfAbsent(tag.getGroup(), key -> new ArrayList<>())
                        .add(TagConverter.toResponse(tagsByName.get(tag.getLabel()))));

        return Arrays.stream(PreferenceTagGroup.values())
                .map(group -> new PreferenceTagGroupResponse(
                        group,
                        group.getLabel(),
                        group.getDescription(),
                        grouped.getOrDefault(group, List.of())
                ))
                .toList();
    }

    @Override
    public TagResponse getTag(Long id) {
        return TagConverter.toResponse(findTagOrThrow(id));
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        String name = normalizeName(request.name());
        validateCatalogName(name, request.type());
        validateDuplicateName(name, request.type());

        try {
            return TagConverter.toResponse(tagRepository.saveAndFlush(Tag.create(name, request.type())));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = findTagOrThrow(id);
        String name = normalizeName(request.name());
        validateCatalogName(name, request.type());

        if (tagRepository.existsByNameAndTypeAndIdNot(name, request.type(), id)) {
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }

        try {
            tag.update(name, request.type());
            return TagConverter.toResponse(tagRepository.saveAndFlush(tag));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = findTagOrThrow(id);

        try {
            tagRepository.delete(tag);
            tagRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.TAG_IN_USE);
        }
    }

    private Tag findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private void validateDuplicateName(String name, TagType type) {
        if (tagRepository.existsByNameAndType(name, type)) {
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }
    }

    private void validateCatalogName(String name, TagType type) {
        if (!TagCatalog.contains(type, name)) {
            throw new CustomException(ErrorCode.TAG_INVALID_NAME);
        }
    }
}

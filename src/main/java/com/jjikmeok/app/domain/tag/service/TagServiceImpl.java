package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.tag.converter.TagConverter;
import com.jjikmeok.app.domain.tag.dto.request.TagDetailReq;
import com.jjikmeok.app.domain.tag.dto.request.TagRegisterReq;
import com.jjikmeok.app.domain.tag.dto.request.TagUpdateReq;
import com.jjikmeok.app.domain.tag.dto.response.TagDetailRes;
import com.jjikmeok.app.domain.tag.dto.response.TagListRes;
import com.jjikmeok.app.domain.tag.dto.response.TagRegisterRes;
import com.jjikmeok.app.domain.tag.dto.response.TagUpdateRes;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public List<TagListRes> getTags(TagType tagType, TagGroupType tagGroupType) {
        return TagConverter.toListRes(findTags(tagType, tagGroupType));
    }

    @Override
    public TagDetailRes getTag(TagDetailReq request) {
        return TagConverter.toDetailRes(findTagOrThrow(request.id()));
    }

    @Override
    @Transactional
    public TagRegisterRes createTag(TagRegisterReq request) {
        String name = normalizeName(request.name());
        validateDuplicateName(name, request.type());

        try {
            return TagConverter.toRegisterRes(tagRepository.saveAndFlush(TagConverter.toEntity(request, name)));
        } catch (DataIntegrityViolationException e) {
            log.warn("태그 생성 실패: 중복된 태그명입니다. name={}, type={}", name, request.type(), e);
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public TagUpdateRes updateTag(Long id, TagUpdateReq request) {
        Tag tag = findTagOrThrow(id);
        String name = normalizeName(request.name());

        if (tagRepository.existsByNameAndTypeAndIdNot(name, request.type(), id)) {
            log.warn("태그 수정 실패: 중복된 태그명입니다. id={}, name={}, type={}", id, name, request.type());
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }

        try {
            TagConverter.updateEntity(tag, request, name);
            return TagConverter.toUpdateRes(tagRepository.saveAndFlush(tag));
        } catch (DataIntegrityViolationException e) {
            log.warn("태그 수정 실패: 중복된 태그명입니다. id={}, name={}, type={}", id, name, request.type(), e);
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
            log.warn("태그 삭제 실패: 사용 중인 태그입니다. id={}", id, e);
            throw new CustomException(ErrorCode.TAG_IN_USE);
        }
    }

    private Tag findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("태그 조회 실패: 존재하지 않는 태그입니다. id={}", id);
                    return new CustomException(ErrorCode.TAG_NOT_FOUND);
                });
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private void validateDuplicateName(String name, TagType type) {
        if (tagRepository.existsByNameAndType(name, type)) {
            log.warn("태그 생성 실패: 중복된 태그명입니다. name={}, type={}", name, type);
            throw new CustomException(ErrorCode.TAG_DUPLICATE_NAME);
        }
    }

    /**
     * 선택적으로 전달된 태그 타입과 선호 태그 그룹 조건에 따라 태그 목록을 조회한다.
     */
    private List<Tag> findTags(TagType type, TagGroupType tagGroupType) {
        if (type != null && tagGroupType != null) {
            return tagRepository.findAllByTypeAndTagGroupTypeOrderByNameAsc(type, tagGroupType);
        }
        if (type != null) {
            return tagRepository.findAllByTypeOrderByNameAsc(type);
        }
        if (tagGroupType != null) {
            return tagRepository.findAllByTagGroupTypeOrderByTypeAscNameAsc(tagGroupType);
        }
        return tagRepository.findAllByOrderByTypeAscNameAsc();
    }
}

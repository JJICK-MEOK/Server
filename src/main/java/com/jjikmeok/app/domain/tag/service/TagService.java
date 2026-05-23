package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.tag.dto.request.TagRequest;
import com.jjikmeok.app.domain.tag.dto.response.TagResponse;
import com.jjikmeok.app.domain.tag.entity.TagType;

import java.util.List;

public interface TagService {

    List<TagResponse> getTags(TagType type);

    TagResponse getTag(Long id);

    TagResponse createTag(TagRequest request);

    TagResponse updateTag(Long id, TagRequest request);

    void deleteTag(Long id);
}

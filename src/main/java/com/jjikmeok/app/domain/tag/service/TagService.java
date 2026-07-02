package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.tag.dto.request.TagDetailReq;
import com.jjikmeok.app.domain.tag.dto.request.TagRegisterReq;
import com.jjikmeok.app.domain.tag.dto.request.TagUpdateReq;
import com.jjikmeok.app.domain.tag.dto.response.TagDetailRes;
import com.jjikmeok.app.domain.tag.dto.response.TagListRes;
import com.jjikmeok.app.domain.tag.dto.response.TagRegisterRes;
import com.jjikmeok.app.domain.tag.dto.response.TagUpdateRes;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;

import java.util.List;

public interface TagService {

    List<TagListRes> getTags(TagType tagType, TagGroupType tagGroupType);

    TagDetailRes getTag(TagDetailReq request);

    TagRegisterRes createTag(TagRegisterReq request);

    TagUpdateRes updateTag(Long id, TagUpdateReq request);

    void deleteTag(Long id);
}

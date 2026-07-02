package com.jjikmeok.app.domain.tag.converter;

import com.jjikmeok.app.domain.tag.dto.request.TagRegisterReq;
import com.jjikmeok.app.domain.tag.dto.request.TagUpdateReq;
import com.jjikmeok.app.domain.tag.dto.response.TagDetailRes;
import com.jjikmeok.app.domain.tag.dto.response.TagListRes;
import com.jjikmeok.app.domain.tag.dto.response.TagRegisterRes;
import com.jjikmeok.app.domain.tag.dto.response.TagUpdateRes;
import com.jjikmeok.app.domain.tag.entity.Tag;

import java.util.List;

public class TagConverter {

    private TagConverter() {
    }

    public static Tag toEntity(TagRegisterReq request, String name) {
        return Tag.create(name, request.type(), request.tagGroupType());
    }

    public static void updateEntity(Tag tag, TagUpdateReq request, String name) {
        tag.update(name, request.type(), request.tagGroupType());
    }

    public static TagListRes toListRes(Tag tag) {
        return new TagListRes(tag.getId(), tag.getName(), tag.getType(), tag.getTagGroupType());
    }

    public static List<TagListRes> toListRes(List<Tag> tags) {
        return tags.stream()
                .map(TagConverter::toListRes)
                .toList();
    }

    public static TagDetailRes toDetailRes(Tag tag) {
        return new TagDetailRes(tag.getId(), tag.getName(), tag.getType(), tag.getTagGroupType());
    }

    public static TagRegisterRes toRegisterRes(Tag tag) {
        return new TagRegisterRes(tag.getId(), tag.getName(), tag.getType(), tag.getTagGroupType());
    }

    public static TagUpdateRes toUpdateRes(Tag tag) {
        return new TagUpdateRes(tag.getId(), tag.getName(), tag.getType(), tag.getTagGroupType());
    }
}

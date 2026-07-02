package com.jjikmeok.app.domain.tag.controller;

import com.jjikmeok.app.domain.tag.dto.request.TagDetailReq;
import com.jjikmeok.app.domain.tag.dto.request.TagRegisterReq;
import com.jjikmeok.app.domain.tag.dto.request.TagUpdateReq;
import com.jjikmeok.app.domain.tag.dto.response.TagDetailRes;
import com.jjikmeok.app.domain.tag.dto.response.TagListRes;
import com.jjikmeok.app.domain.tag.dto.response.TagRegisterRes;
import com.jjikmeok.app.domain.tag.dto.response.TagUpdateRes;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.service.TagService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tag", description = "태그 사전 관리 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 목록 조회")
    @GetMapping
    public ApiResponse<List<TagListRes>> getTags(
            @Parameter(description = "태그 타입", example = "PREFERENCE_TAG")
            @RequestParam(value = "tagType", required = false) TagType tagType,
            @Parameter(description = "태그 그룹 타입", example = "MOOD")
            @RequestParam(value = "tagGroupType", required = false) TagGroupType tagGroupType) {
        return ApiResponse.success("태그 목록 조회 성공", tagService.getTags(tagType, tagGroupType));
    }

    @Operation(summary = "태그 상세 조회")
    @GetMapping("/{tagId}")
    public ApiResponse<TagDetailRes> getTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable("tagId") @Positive(message = "태그 ID는 양수여야 합니다.") Long id) {
        return ApiResponse.success("태그 상세 조회 성공", tagService.getTag(new TagDetailReq(id)));
    }

    @Operation(summary = "태그 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagRegisterRes> createTag(@RequestBody @Valid TagRegisterReq request) {
        return ApiResponse.success("태그 생성 성공", tagService.createTag(request));
    }

    @Operation(summary = "태그 수정")
    @PutMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagUpdateRes> updateTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable("tagId") @Positive(message = "태그 ID는 양수여야 합니다.") Long id,
            @RequestBody @Valid TagUpdateReq request) {
        return ApiResponse.success("태그 수정 성공", tagService.updateTag(id, request));
    }

    @Operation(summary = "태그 삭제")
    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTag(
            @Parameter(description = "태그 ID", example = "1")
            @PathVariable("tagId") @Positive(message = "태그 ID는 양수여야 합니다.") Long id) {
        tagService.deleteTag(id);
    }
}

package com.jjikmeok.app.domain.tag.controller;

import com.jjikmeok.app.domain.tag.dto.request.TagRequest;
import com.jjikmeok.app.domain.tag.dto.response.TagResponse;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.service.TagService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 목록 조회")
    @GetMapping
    public ApiResponse<List<TagResponse>> getTags(
            @RequestParam(value = "type", required = false) TagType type) {
        return ApiResponse.success("태그 목록 조회 성공", tagService.getTags(type));
    }

    @Operation(summary = "태그 상세 조회")
    @GetMapping("/{tagId}")
    public ApiResponse<TagResponse> getTag(@PathVariable("tagId") Long id) {
        return ApiResponse.success("태그 상세 조회 성공", tagService.getTag(id));
    }

    @Operation(summary = "태그 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> createTag(@RequestBody @Valid TagRequest request) {
        return ApiResponse.success("태그 생성 성공", tagService.createTag(request));
    }

    @Operation(summary = "태그 수정")
    @PutMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> updateTag(
            @PathVariable("tagId") Long id,
            @RequestBody @Valid TagRequest request) {
        return ApiResponse.success("태그 수정 성공", tagService.updateTag(id, request));
    }

    @Operation(summary = "태그 삭제")
    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTag(@PathVariable("tagId") Long id) {
        tagService.deleteTag(id);
    }
}

package com.jjikmeok.app.domain.s3.controller;

import com.example.project.domain.image.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final S3ImageService s3ImageService;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestPart("file") MultipartFile file
    ) {
        String key = s3ImageService.uploadImage(file, "images");

        return ResponseEntity.ok(Map.of(
                "key", key
        ));
    }
}
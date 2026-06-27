package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityAttachmentStorageService {

    private static final Pattern DATA_IMAGE = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);

    @Value("${app.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    public String uploadDataImage(SourceType sourceType, String externalId, String dataUri) {
        if (dataUri == null || dataUri.isBlank()) return null;

        Matcher matcher = DATA_IMAGE.matcher(dataUri);
        if (!matcher.matches()) return null;

        String contentType = matcher.group(1).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try {
            // TODO: 로컬 정적 파일 저장소를 S3 업로드로 대체하고 S3/CDN URL을 반환하도록 수정해야 함.
            bytes = Base64.getMimeDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException e) {
            log.warn("활동 첨부파일 base64 디코딩 실패. sourceType={}, externalId={}", sourceType, externalId);
            return null;
        }

        String dirName = sourceType.name().toLowerCase(Locale.ROOT);
        String fileName = "%s-%s.%s".formatted(safe(externalId), hash(dataUri).substring(0, 16), extension(contentType));
        String relativePath = "/images/activities/" + dirName + "/" + fileName;

        try {
            String projectPath = System.getProperty("user.dir");
            File uploadDir = new File(projectPath, "src/main/resources/static/images/activities/" + dirName);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File uploadFile = new File(uploadDir, fileName);
            if (!uploadFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
                    fos.write(bytes);
                }
            }
            return serverBaseUrl + relativePath;
        } catch (Exception e) {
            log.warn("로컬 이미지 저장 실패. sourceType={}, externalId={}, message={}", sourceType, externalId, e.getMessage());
            return null;
        }
    }

    private String safe(String value) {
        return (value == null || value.isBlank() ? "unknown" : value).replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
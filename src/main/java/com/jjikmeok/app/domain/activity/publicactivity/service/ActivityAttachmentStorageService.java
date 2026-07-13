package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.global.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityAttachmentStorageService {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_BASE64_CHARS = ((MAX_IMAGE_BYTES + 2) / 3) * 4 + 1024;

    private static final Pattern DATA_IMAGE = Pattern.compile(
            "^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$",
            Pattern.DOTALL
    );

    private final StorageService storageService;

    public String uploadDataImage(SourceType sourceType, String externalId, String dataUri) {
        if (sourceType == null || dataUri == null || dataUri.isBlank()) {
            return null;
        }

        Matcher matcher = DATA_IMAGE.matcher(dataUri);
        if (!matcher.matches()) {
            return null;
        }

        String contentType = matcher.group(1).toLowerCase(Locale.ROOT);
        String extension = extension(contentType);
        String encodedContent = matcher.group(2);
        if (extension == null || encodedContent.length() > MAX_BASE64_CHARS) {
            log.warn("Activity image validation failed. sourceType={}, externalId={}, contentType={}",
                    sourceType, externalId, contentType);
            return null;
        }

        byte[] content;
        try {
            content = Base64.getMimeDecoder().decode(encodedContent);
        } catch (IllegalArgumentException e) {
            log.warn("Activity image base64 decoding failed. sourceType={}, externalId={}", sourceType, externalId);
            return null;
        }

        if (content.length == 0 || content.length > MAX_IMAGE_BYTES || !matchesFileSignature(contentType, content)) {
            log.warn("Activity image validation failed. sourceType={}, externalId={}, contentType={}, size={}",
                    sourceType, externalId, contentType, content.length);
            return null;
        }

        String directory = sourceType.name().toLowerCase(Locale.ROOT);
        String fileName = UUID.randomUUID() + "." + extension;
        String objectName = "images/activities/" + directory + "/" + fileName;

        try {
            return storageService.store(objectName, content, contentType);
        } catch (RuntimeException e) {
            log.warn(
                    "Activity image storage failed. sourceType={}, externalId={}, message={}",
                    sourceType,
                    externalId,
                    e.getMessage()
            );
            return null;
        }
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    private boolean matchesFileSignature(String contentType, byte[] content) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/gif" -> startsWith(content, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(content, 'G', 'I', 'F', '8', '9', 'a');
            case "image/webp" -> startsWith(content, 'R', 'I', 'F', 'F')
                    && startsWithAt(content, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, int... signature) {
        return startsWithAt(content, 0, signature);
    }

    private boolean startsWithAt(byte[] content, int offset, int... signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[offset + index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }
}

package com.jjikmeok.app.global.infra.storage;

final class StorageInputValidator {

    private StorageInputValidator() {
    }

    static void validate(String objectName, byte[] content, String contentType) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name must not be blank");
        }
        if (objectName.startsWith("/") || objectName.contains("\\") || objectName.contains("..")) {
            throw new IllegalArgumentException("Object name must be a safe relative path");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Object content must not be empty");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type must not be blank");
        }
    }
}

package com.jjikmeok.app.global.infra.storage;

public interface StorageService {
    String store(String objectName, byte[] content, String contentType);
}

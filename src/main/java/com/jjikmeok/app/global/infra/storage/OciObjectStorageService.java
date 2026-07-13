package com.jjikmeok.app.global.infra.storage;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@Profile("prod")
@ConditionalOnProperty(name = "storage.type", havingValue = "oci")
@EnableConfigurationProperties(StorageProperties.class)
public class OciObjectStorageService implements StorageService {
    private final ObjectStorage objectStorage;
    private final String namespace;
    private final String bucket;
    private final String publicBaseUrl;

    public OciObjectStorageService(ObjectStorage objectStorage, StorageProperties properties) {
        this.objectStorage = objectStorage;
        this.namespace = required(properties.getOci().getNamespace(), "OCI Object Storage namespace");
        this.bucket = required(properties.getOci().getBucket(), "OCI Object Storage bucket");
        this.publicBaseUrl = trimTrailingSlash(required(
                properties.getOci().getPublicBaseUrl(),
                "OCI Object Storage public base URL"
        ));
    }

    @Override
    public String store(String objectName, byte[] content, String contentType) {
        StorageInputValidator.validate(objectName, content, contentType);

        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectName)
                .contentType(contentType)
                .contentLength((long) content.length)
                .putObjectBody(new ByteArrayInputStream(content))
                .build();
        try {
            objectStorage.putObject(request);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to upload object to OCI Object Storage", e);
        }
        return publicBaseUrl + "/" + objectName;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}

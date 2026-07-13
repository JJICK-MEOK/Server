package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.global.infra.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAttachmentStorageServiceTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
    };

    @Mock
    private StorageService storageService;

    @Test
    void uploadDataImage_storesWithUuidKeyAndReturnsStorageUrl() {
        ActivityAttachmentStorageService service = new ActivityAttachmentStorageService(storageService);
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG);
        when(storageService.store(anyString(), aryEq(PNG), eq("image/png")))
                .thenReturn("https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/ns/b/bucket/o/object.png");

        String result = service.uploadDataImage(SourceType.KOPIS, "untrusted/name.png", dataUri);

        assertThat(result).isEqualTo(
                "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/ns/b/bucket/o/object.png");
        verify(storageService).store(
                org.mockito.ArgumentMatchers.matches(
                        "images/activities/kopis/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png"),
                aryEq(PNG),
                eq("image/png")
        );
    }

    @Test
    void uploadDataImage_rejectsUnsupportedMimeType() {
        ActivityAttachmentStorageService service = new ActivityAttachmentStorageService(storageService);

        String result = service.uploadDataImage(
                SourceType.KOPIS,
                "id",
                "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString("<svg/>".getBytes())
        );

        assertThat(result).isNull();
        verify(storageService, never()).store(anyString(), org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void uploadDataImage_rejectsMimeAndFileSignatureMismatch() {
        ActivityAttachmentStorageService service = new ActivityAttachmentStorageService(storageService);
        byte[] notPng = "not-a-png".getBytes();

        String result = service.uploadDataImage(
                SourceType.KOPIS,
                "id",
                "data:image/png;base64," + Base64.getEncoder().encodeToString(notPng)
        );

        assertThat(result).isNull();
        verify(storageService, never()).store(anyString(), org.mockito.ArgumentMatchers.any(), anyString());
    }
}

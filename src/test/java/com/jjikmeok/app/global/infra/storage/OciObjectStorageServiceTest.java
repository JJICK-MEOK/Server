package com.jjikmeok.app.global.infra.storage;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OciObjectStorageServiceTest {

    @Mock
    private ObjectStorage objectStorage;

    @Test
    void store_uploadsObjectAndReturnsFixedPublicUrl() {
        StorageProperties properties = properties();
        OciObjectStorageService service = new OciObjectStorageService(objectStorage, properties);
        byte[] content = {1, 2, 3};

        String url = service.store("images/activities/kopis/id.png", content, "image/png");

        assertThat(url).isEqualTo(
                "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/test-ns/b/public-images/o/images/activities/kopis/id.png");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(objectStorage).putObject(captor.capture());
        PutObjectRequest request = captor.getValue();
        assertThat(request.getNamespaceName()).isEqualTo("test-ns");
        assertThat(request.getBucketName()).isEqualTo("public-images");
        assertThat(request.getObjectName()).isEqualTo("images/activities/kopis/id.png");
        assertThat(request.getContentType()).isEqualTo("image/png");
        assertThat(request.getContentLength()).isEqualTo(3L);
    }

    @Test
    void store_rejectsUnsafeObjectName() {
        OciObjectStorageService service = new OciObjectStorageService(objectStorage, properties());

        assertThatThrownBy(() -> service.store("../secret.png", new byte[]{1}, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object name must be a safe relative path");
    }

    private StorageProperties properties() {
        StorageProperties properties = new StorageProperties();
        properties.getOci().setNamespace("test-ns");
        properties.getOci().setBucket("public-images");
        properties.getOci().setPublicBaseUrl(
                "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/test-ns/b/public-images/o/");
        return properties;
    }
}

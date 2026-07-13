package com.jjikmeok.app.global.infra.storage;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Locale;

@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "storage.type", havingValue = "oci")
@EnableConfigurationProperties(StorageProperties.class)
public class OciObjectStorageConfiguration {
    @Bean(destroyMethod = "close")
    ObjectStorage objectStorage(StorageProperties properties) {
        StorageProperties.Oci oci = properties.getOci();
        validateInstancePrincipal(oci.getAuthType());
        InstancePrincipalsAuthenticationDetailsProvider provider =
                InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        ObjectStorage client = ObjectStorageClient.builder().build(provider);
        client.setRegion(Region.fromRegionId(required(oci.getRegion(), "OCI region")));
        return client;
    }

    private void validateInstancePrincipal(String configuredAuthType) {
        String authType = required(configuredAuthType, "OCI auth type").toUpperCase(Locale.ROOT);
        if (!"INSTANCE_PRINCIPAL".equals(authType)) {
            throw new IllegalArgumentException("Unsupported OCI auth type: " + authType);
        }
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

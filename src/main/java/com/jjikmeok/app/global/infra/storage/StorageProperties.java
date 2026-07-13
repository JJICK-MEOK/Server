package com.jjikmeok.app.global.infra.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String type = "local";
    private Local local = new Local();
    private Oci oci = new Oci();

    @Getter
    @Setter
    public static class Local {
        private String directory = "./data/uploads";
        private String publicBaseUrl = "http://localhost:8080/uploads";
    }

    @Getter
    @Setter
    public static class Oci {
        private String region;
        private String namespace;
        private String bucket;
        private String publicBaseUrl;
        private String authType = "INSTANCE_PRINCIPAL";
    }
}

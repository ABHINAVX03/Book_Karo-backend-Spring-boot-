package com.codingshuttle.project.uber.uberApp.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class AppStorageProperties {

    /** Comma-separated fallback providers after Cloudinary: local, s3 */
    private String fallbacks = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        private String directory = "uploads";
        /** Public base URL for stored files, e.g. http://localhost:8080/uploads */
        private String baseUrl = "http://localhost:8080/uploads";
    }

    @Getter
    @Setter
    public static class S3 {
        private boolean enabled = false;
        private String bucket;
        private String region = "ap-south-1";
        private String publicBaseUrl;
    }
}

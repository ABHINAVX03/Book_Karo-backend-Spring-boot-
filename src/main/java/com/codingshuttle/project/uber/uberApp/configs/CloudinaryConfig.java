package com.codingshuttle.project.uber.uberApp.configs;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", clean(cloudName));
        config.put("api_key", clean(apiKey));
        config.put("api_secret", clean(apiSecret));
        return new Cloudinary(config);
    }

    private String clean(String value) {
        if (value == null) return "";
        // Remove quotes if present and trim whitespace
        return value.trim().replaceAll("^\"|\"$", "");
    }
}

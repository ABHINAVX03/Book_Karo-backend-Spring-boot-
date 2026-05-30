package com.codingshuttle.project.uber.uberApp.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.dev")
public class AppDevProperties {

    /** When true, exposes /dev/** testing endpoints (never enable in production) */
    private boolean endpointsEnabled = false;
}

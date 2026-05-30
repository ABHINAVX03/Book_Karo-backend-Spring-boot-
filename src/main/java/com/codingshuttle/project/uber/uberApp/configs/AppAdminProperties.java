package com.codingshuttle.project.uber.uberApp.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AppAdminProperties {

    /** Email address notified when a driver submits verification documents */
    private String notificationEmail = "";
}

package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.configs.AppDevProperties;
import com.codingshuttle.project.uber.uberApp.services.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev/drivers")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev", name = "endpoints-enabled", havingValue = "true")
public class DevDriverController {

    private final DriverService driverService;
    private final AppDevProperties appDevProperties;

    /**
     * Instantly approves a driver for local/testing workflows.
     * Only registered when app.dev.endpoints-enabled=true (dev profile).
     */
    @PostMapping("/{id}/auto-approve")
    @Secured({"ROLE_ADMIN", "ROLE_DRIVER"})
    public ResponseEntity<Void> autoApproveDriver(@PathVariable Long id) {
        if (!appDevProperties.isEndpointsEnabled()) {
            throw new RuntimeException("Dev endpoints are disabled");
        }
        driverService.autoApproveDriverForDev(id);
        return ResponseEntity.ok().build();
    }
}

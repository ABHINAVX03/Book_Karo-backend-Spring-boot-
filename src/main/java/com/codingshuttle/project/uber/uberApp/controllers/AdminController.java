package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.DriverVerificationDto;
import com.codingshuttle.project.uber.uberApp.services.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    private final DriverService driverService;

    @GetMapping("/drivers")
    public ResponseEntity<Page<DriverVerificationDto>> getAllDriversByStatus(
            @RequestParam(defaultValue = "PENDING") com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus status,
            @RequestParam(defaultValue = "0") Integer pageOffset,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize, Sort.by("id").descending());
        return ResponseEntity.ok(driverService.getAllDriversByStatus(status, pageRequest));
    }

    @GetMapping("/drivers/pending")
    public ResponseEntity<Page<DriverVerificationDto>> getPendingDrivers(
            @RequestParam(defaultValue = "0") Integer pageOffset,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize, Sort.by("id").descending());
        return ResponseEntity.ok(driverService.getPendingDrivers(pageRequest));
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<DriverVerificationDto> getDriverDetails(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverVerificationDetails(id));
    }

    @PutMapping("/drivers/{id}/approve")
    public ResponseEntity<Void> approveDriver(@PathVariable Long id) {
        driverService.approveDriver(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/drivers/{id}/reject")
    public ResponseEntity<Void> rejectDriver(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("rejectionReason", "Documents missing or invalid");
        driverService.rejectDriver(id, reason);
        return ResponseEntity.ok().build();
    }
}
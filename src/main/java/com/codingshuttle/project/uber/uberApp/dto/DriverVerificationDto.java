package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverVerificationDto {
    private Long id;
    private UserDto user;
    private String vehicleId;
    private String vehicleModel;
    private DriverVerificationStatus verificationStatus;
    private String rcUrl;
    private String licenseUrl;
    private String insuranceUrl;
    private String profilePhotoUrl;
    private String rejectionReason;
}

package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {
    private Long id;
    private UserDto user;
    private Double rating;
    private Boolean available;
    private String vehicleId;
    private VehicleType vehicleType;
    private Boolean vehicleVerified;
    private Boolean blocked;
    private com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus verificationStatus;
    private String rcUrl;
    private String licenseUrl;
    private String insuranceUrl;
    private String profilePhotoUrl;
    private String rejectionReason;
}

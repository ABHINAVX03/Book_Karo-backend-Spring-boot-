package com.codingshuttle.project.uber.uberApp.mappers;

import com.codingshuttle.project.uber.uberApp.dto.DriverVerificationDto;
import com.codingshuttle.project.uber.uberApp.dto.UserDto;
import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.User;
import org.springframework.stereotype.Component;

/**
 * Explicit mapper — avoids ModelMapper failures when mapping {@link Driver}
 * (lazy {@link User} graph, geometry fields, bidirectional associations).
 */
@Component
public class DriverVerificationMapper {

    public DriverVerificationDto toDto(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverVerificationDto.builder()
                .id(driver.getId())
                .user(toUserDto(driver.getUser()))
                .vehicleId(driver.getVehicleId())
                .vehicleModel(driver.getVehicleModel())
                .verificationStatus(driver.getVerificationStatus())
                .rcUrl(driver.getRcUrl())
                .licenseUrl(driver.getLicenseUrl())
                .insuranceUrl(driver.getInsuranceUrl())
                .profilePhotoUrl(driver.getProfilePhotoUrl())
                .rejectionReason(driver.getRejectionReason())
                .available(driver.getAvailable())
                .blocked(driver.getBlocked())
                .verificationSubmitted(driver.getVerificationSubmitted())
                .vehicleVerified(driver.getVehicleVerified())
                .build();
    }

    private UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRoles()
        );
    }
}

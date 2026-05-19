package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardDriverDto {
    @NotBlank(message = "Vehicle id is required")
    @Size(max = 40, message = "Vehicle id must be at most 40 characters")
    private String vehicleId;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in international format")
    private String phoneNumber;
}

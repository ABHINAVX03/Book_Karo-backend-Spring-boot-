package com.codingshuttle.project.uber.uberApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RideStartDto {
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP must contain 4 to 8 digits")
    String otp;
}

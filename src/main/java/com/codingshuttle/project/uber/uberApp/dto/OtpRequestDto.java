package com.codingshuttle.project.uber.uberApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpRequestDto {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in international format")
    private String phoneNumber;
}

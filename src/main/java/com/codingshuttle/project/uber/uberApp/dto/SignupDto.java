package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupDto {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 180, message = "Email must be at most 180 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in international format")
    private String phoneNumber;

    private Role role;
}

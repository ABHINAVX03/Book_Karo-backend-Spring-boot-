package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.dto.DriverDto;
import com.codingshuttle.project.uber.uberApp.dto.SignupDto;
import com.codingshuttle.project.uber.uberApp.dto.UserDto;
import com.codingshuttle.project.uber.uberApp.dto.UpdateProfileDto;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;

public interface AuthService {

    String[] login(String email, String password);

    UserDto signup(SignupDto signupDto);

    UserDto updateProfile(Long userId, UpdateProfileDto dto);

    DriverDto onboardNewDriver(Long userId, String vehicleId, VehicleType vehicleType);

    String refreshToken(String refreshToken);

    UserDto getUserByEmail(String email);
}

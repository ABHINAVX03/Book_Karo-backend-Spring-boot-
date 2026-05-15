package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.codingshuttle.project.uber.uberApp.services.OtpService otpService;

    @Value("${app.security.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    @Value("${app.security.refresh-cookie-same-site:None}")
    private String refreshCookieSameSite;

    @PostMapping("/signup")
    ResponseEntity<UserDto> signUp(@RequestBody SignupDto signupDto) {
       return new ResponseEntity<>(authService.signup(signupDto), HttpStatus.CREATED);
    }

//    @Secured("ROLE_ADMIN")
    @PostMapping("/onBoardNewDriver/{userId}")
    ResponseEntity<DriverDto> onBoardNewDriver(@PathVariable Long userId, @RequestBody OnboardDriverDto onboardDriverDto) {
        return new ResponseEntity<>(authService.onboardNewDriver(userId,
                onboardDriverDto.getVehicleId(), onboardDriverDto.getVehicleType()), HttpStatus.CREATED);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(@RequestBody OtpRequestDto otpRequestDto) {
        otpService.sendOtp(otpRequestDto.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<java.util.Map<String, Boolean>> verifyOtp(@RequestBody OtpVerifyDto otpVerifyDto) {
        boolean isValid = otpService.verifyOtp(otpVerifyDto.getPhoneNumber(), otpVerifyDto.getOtp());
        return ResponseEntity.ok(java.util.Map.of("valid", isValid));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto,
                                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String tokens[] = authService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens[1])
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(180))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        UserDto userDto = authService.getUserByEmail(loginRequestDto.getEmail());
        return ResponseEntity.ok(new LoginResponseDto(tokens[0], userDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AuthenticationServiceException("Refresh token not found inside the Cookies");
        }

        String refreshToken = Arrays.stream(cookies).
                filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        String accessToken = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(new LoginResponseDto(accessToken, null));
    }


}

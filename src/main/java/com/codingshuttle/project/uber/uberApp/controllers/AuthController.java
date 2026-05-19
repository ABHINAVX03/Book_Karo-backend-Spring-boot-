package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.*;
import jakarta.validation.Valid;
import com.codingshuttle.project.uber.uberApp.services.AuthService;
import com.codingshuttle.project.uber.uberApp.services.CaptchaVerificationService;
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
    private final CaptchaVerificationService captchaVerificationService;

    @Value("${app.security.access-cookie-secure:true}")
    private boolean accessCookieSecure;

    @Value("${app.security.access-cookie-same-site:Lax}")
    private String accessCookieSameSite;

    @Value("${app.security.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    @Value("${app.security.refresh-cookie-same-site:None}")
    private String refreshCookieSameSite;

    @PostMapping("/signup")
    ResponseEntity<UserDto> signUp(@Valid @RequestBody SignupDto signupDto) {
       return new ResponseEntity<>(authService.signup(signupDto), HttpStatus.CREATED);
    }

//    @Secured("ROLE_ADMIN")
    @PostMapping("/onBoardNewDriver/{userId}")
    ResponseEntity<DriverDto> onBoardNewDriver(@PathVariable Long userId, @Valid @RequestBody OnboardDriverDto onboardDriverDto) {
        return new ResponseEntity<>(authService.onboardNewDriver(userId,
                onboardDriverDto.getVehicleId(), onboardDriverDto.getVehicleType(), 
                onboardDriverDto.getPhoneNumber()), HttpStatus.CREATED);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody OtpRequestDto otpRequestDto, HttpServletRequest request) {
        captchaVerificationService.assertValidCaptcha(request);
        otpService.sendOtp(otpRequestDto.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<java.util.Map<String, Boolean>> verifyOtp(@Valid @RequestBody OtpVerifyDto otpVerifyDto, HttpServletRequest request) {
        captchaVerificationService.assertValidCaptcha(request);
        boolean isValid = otpService.verifyOtp(otpVerifyDto.getPhoneNumber(), otpVerifyDto.getOtp());
        return ResponseEntity.ok(java.util.Map.of("valid", isValid));
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
                                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        captchaVerificationService.assertValidCaptcha(httpServletRequest);
        AuthTokensDto tokens = authService.login(
                loginRequestDto.getEmail(),
                loginRequestDto.getPassword(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        );

        addAccessTokenCookie(httpServletResponse, tokens.getAccessToken());
        addRefreshTokenCookie(httpServletResponse, tokens.getRefreshToken());

        return ResponseEntity.ok(new LoginResponseDto(tokens.getAccessToken(), tokens.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        AuthTokensDto tokens = authService.refreshToken(
                refreshToken,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        addAccessTokenCookie(response, tokens.getAccessToken());
        addRefreshTokenCookie(response, tokens.getRefreshToken());

        return ResponseEntity.ok(new LoginResponseDto(tokens.getAccessToken(), tokens.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(extractRefreshToken(request));
        } catch (Exception ignored) {
            // Cookie revocation should still proceed on malformed/expired tokens.
        }
        clearCookie(response, "accessToken", accessCookieSecure, accessCookieSameSite);
        clearCookie(response, "refreshToken", refreshCookieSecure, refreshCookieSameSite);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AuthenticationServiceException("Refresh token not found inside the Cookies");
        }

        return Arrays.stream(cookies)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));
    }

    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(accessCookieSecure)
                .sameSite(accessCookieSameSite)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(180))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String cookieName, boolean secure, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

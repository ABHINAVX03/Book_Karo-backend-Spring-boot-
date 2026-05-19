package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
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
    private final AppSecurityProperties appSecurityProperties;

    @Value("${app.security.access-cookie-secure:true}")
    private boolean accessCookieSecure;

    @Value("${app.security.access-cookie-same-site:None}")
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

        boolean accessSecure = useSecureCookie(httpServletRequest, accessCookieSecure);
        boolean refreshSecure = useSecureCookie(httpServletRequest, refreshCookieSecure);
        addAccessTokenCookie(httpServletResponse, tokens.getAccessToken(), accessSecure);
        addRefreshTokenCookie(httpServletResponse, tokens.getRefreshToken(), refreshSecure);

        return ResponseEntity.ok(new LoginResponseDto(tokens.getAccessToken(), tokens.getRefreshToken(), tokens.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response,
                                                     @RequestBody(required = false) RefreshTokenDto refreshTokenDto) {
        String refreshToken = extractRefreshToken(request, refreshTokenDto);
        AuthTokensDto tokens = authService.refreshToken(
                refreshToken,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        boolean accessSecure = useSecureCookie(request, accessCookieSecure);
        boolean refreshSecure = useSecureCookie(request, refreshCookieSecure);
        addAccessTokenCookie(response, tokens.getAccessToken(), accessSecure);
        addRefreshTokenCookie(response, tokens.getRefreshToken(), refreshSecure);

        return ResponseEntity.ok(new LoginResponseDto(tokens.getAccessToken(), tokens.getRefreshToken(), tokens.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response,
                                       @RequestBody(required = false) RefreshTokenDto refreshTokenDto) {
        try {
            authService.logout(extractRefreshToken(request, refreshTokenDto));
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

    private void addAccessTokenCookie(HttpServletResponse response, String token, boolean secure) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(accessCookieSameSite)
                .path("/")
                .maxAge(Duration.ofMinutes(appSecurityProperties.getAccessTokenMinutes()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token, boolean secure) {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(appSecurityProperties.getRefreshTokenDays()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String extractRefreshToken(HttpServletRequest request, RefreshTokenDto refreshTokenDto) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        if (refreshTokenDto != null && refreshTokenDto.getRefreshToken() != null && !refreshTokenDto.getRefreshToken().isBlank()) {
            return refreshTokenDto.getRefreshToken();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        throw new AuthenticationServiceException("Refresh token not found inside the request");
    }

    private boolean useSecureCookie(HttpServletRequest request, boolean configuredSecure) {
        if (!configuredSecure) {
            return false;
        }
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Ssl"));
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

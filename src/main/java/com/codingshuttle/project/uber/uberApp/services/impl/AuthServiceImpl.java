package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
import com.codingshuttle.project.uber.uberApp.dto.AuthTokensDto;
import com.codingshuttle.project.uber.uberApp.dto.DriverDto;
import com.codingshuttle.project.uber.uberApp.dto.SignupDto;
import com.codingshuttle.project.uber.uberApp.dto.UpdateProfileDto;
import com.codingshuttle.project.uber.uberApp.dto.UserDto;
import com.codingshuttle.project.uber.uberApp.entities.AuthSession;
import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.enums.Role;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.exceptions.RuntimeConflictException;
import com.codingshuttle.project.uber.uberApp.exceptions.UnauthorizedAccessException;
import com.codingshuttle.project.uber.uberApp.repositories.DriverRepository;
import com.codingshuttle.project.uber.uberApp.repositories.UserRepository;
import com.codingshuttle.project.uber.uberApp.security.JWTService;
import com.codingshuttle.project.uber.uberApp.services.AuthService;
import com.codingshuttle.project.uber.uberApp.services.DriverService;
import com.codingshuttle.project.uber.uberApp.services.OtpService;
import com.codingshuttle.project.uber.uberApp.services.RiderService;
import com.codingshuttle.project.uber.uberApp.services.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static com.codingshuttle.project.uber.uberApp.entities.enums.Role.DRIVER;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final ModelMapper modelMapper;
    private final RiderService riderService;
    private final WalletService walletService;
    private final DriverService driverService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final OtpService otpService;
    private final AuthSessionService authSessionService;
    private final AppSecurityProperties appSecurityProperties;

    @Override
    @Transactional
    public AuthTokensDto login(String email, String password, String clientIp, String userAgent) {
        User account = userRepository.findByEmailForUpdate(email).orElse(null);
        if (account != null && !account.isAccountNonLocked()) {
            log.warn("Login blocked for locked account email={} ip={}", email, clientIp);
            throw new LockedException("Account is temporarily locked. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = (User) authentication.getPrincipal();
            resetFailedLoginState(user);

            JWTService.TokenDetails accessToken = jwtService.generateAccessToken(user);
            JWTService.TokenDetails refreshToken = jwtService.generateRefreshToken(user);
            authSessionService.createSession(user, refreshToken, clientIp, userAgent);

            log.info("Login succeeded for userId={} email={} ip={}", user.getId(), user.getEmail(), clientIp);
            return new AuthTokensDto(accessToken.token(), refreshToken.token(), modelMapper.map(user, UserDto.class));
        } catch (BadCredentialsException ex) {
            registerFailedLogin(email, clientIp);
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserDto signup(SignupDto signupDto) {
        User user = userRepository.findByEmail(signupDto.getEmail()).orElse(null);
        if (user != null) {
            throw new RuntimeConflictException("Cannot signup, User already exists with email " + signupDto.getEmail());
        }

        if (!otpService.isPhoneNumberVerified(signupDto.getPhoneNumber())) {
            throw new RuntimeConflictException("Phone number " + signupDto.getPhoneNumber() + " is not verified. Verification is mandatory.");
        }

        User mappedUser = modelMapper.map(signupDto, User.class);
        Role role = signupDto.getRole() != null ? signupDto.getRole() : Role.RIDER;
        mappedUser.setRoles(Set.of(role));
        mappedUser.setPassword(passwordEncoder.encode(mappedUser.getPassword()));
        mappedUser.setIsVerified(true);
        mappedUser.setFailedLoginAttempts(0);
        mappedUser.setLockedUntil(null);
        User savedUser = userRepository.save(mappedUser);

        if (role == Role.RIDER) {
            riderService.createNewRider(savedUser);
        }
        walletService.createNewWallet(savedUser);

        log.info("Signup completed for userId={} role={}", savedUser.getId(), role);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    @Transactional
    public DriverDto onboardNewDriver(Long userId, String vehicleId, VehicleType vehicleType, String phoneNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User currentUser)) {
            throw new UnauthorizedAccessException("Sign in is required to register your vehicle.");
        }
        if (!currentUser.getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only complete vehicle registration for your own account.");
        }

        if (!otpService.isPhoneNumberVerified(phoneNumber)) {
            throw new RuntimeConflictException("Phone number " + phoneNumber + " is not verified. Driver activation requires verification.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (user.getRoles().contains(Role.RIDER)) {
            throw new RuntimeConflictException("User with id " + userId + " is a Rider. Role isolation is enabled.");
        }

        if (driverRepository.findByUser(user).isPresent()) {
            throw new RuntimeConflictException("Driver profile already exists for user with id " + userId);
        }

        user.setPhoneNumber(phoneNumber);
        user.setIsVerified(true);

        Driver createDriver = Driver.builder()
                .user(user)
                .rating(0.0)
                .vehicleId(vehicleId)
                .vehicleType(vehicleType)
                .available(true)
                .build();

        user.setRoles(Set.of(DRIVER));
        userRepository.save(user);
        otpService.clearVerification(phoneNumber);

        Driver savedDriver = driverService.createNewDriver(createDriver);
        return modelMapper.map(savedDriver, DriverDto.class);
    }

    @Override
    @Transactional
    public AuthTokensDto refreshToken(String refreshToken, String clientIp, String userAgent) {
        JWTService.ParsedToken parsedToken = jwtService.parseToken(refreshToken);
        AuthSession session = authSessionService.validateRefreshSession(parsedToken, refreshToken);
        User user = userRepository.findById(parsedToken.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + parsedToken.userId()));

        JWTService.TokenDetails newAccessToken = jwtService.generateAccessToken(user);
        JWTService.TokenDetails newRefreshToken = jwtService.generateRefreshToken(user);
        authSessionService.revoke(session, newRefreshToken.jti());
        authSessionService.createSession(user, newRefreshToken, clientIp, userAgent);

        log.info("Refresh token rotated for userId={} ip={}", user.getId(), clientIp);
        return new AuthTokensDto(newAccessToken.token(), newRefreshToken.token(), modelMapper.map(user, UserDto.class));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        JWTService.ParsedToken parsedToken = jwtService.parseToken(refreshToken);
        authSessionService.revokeByParsedToken(parsedToken);
        log.info("Refresh session revoked for userId={}", parsedToken.userId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return modelMapper.map(userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user.getId())), UserDto.class);
    }

    private void registerFailedLogin(String email, String clientIp) {
        userRepository.findByEmailForUpdate(email).ifPresent(user -> {
            int failedAttempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= appSecurityProperties.getLoginMaxAttempts()) {
                user.setLockedUntil(LocalDateTime.now().plus(appSecurityProperties.getLockoutDuration()));
                user.setFailedLoginAttempts(0);
                log.warn("Account locked after repeated failures userId={} email={} ip={}", user.getId(), email, clientIp);
            } else {
                log.warn("Failed login for email={} ip={} attempts={}", email, clientIp, failedAttempts);
            }
            userRepository.save(user);
        });
    }

    private void resetFailedLoginState(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}

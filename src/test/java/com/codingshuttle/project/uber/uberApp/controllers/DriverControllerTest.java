package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.TestContainerConfiguration;
import com.codingshuttle.project.uber.uberApp.dto.RideStartDto;
import com.codingshuttle.project.uber.uberApp.entities.*;
import com.codingshuttle.project.uber.uberApp.entities.enums.*;
import com.codingshuttle.project.uber.uberApp.repositories.*;
import com.codingshuttle.project.uber.uberApp.security.JWTService;
import com.codingshuttle.project.uber.uberApp.utils.GeometryUtil;
import com.codingshuttle.project.uber.uberApp.dto.PointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureWebTestClient(timeout = "100000")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DriverControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JWTService jwtService;

    private User driverUser;
    private Driver driver;
    private Rider rider;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        riderRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Driver User
        driverUser = new User();
        driverUser.setName("Driver User");
        driverUser.setEmail("driver@example.com");
        driverUser.setPhoneNumber("+919876543210");
        driverUser.setPassword("password");
        driverUser.setIsVerified(true);
        driverUser.setRoles(Set.of(Role.DRIVER));
        driverUser = userRepository.save(driverUser);

        // 2. Create Driver
        driver = new Driver();
        driver.setUser(driverUser);
        driver.setRating(4.9);
        driver.setAvailable(true);
        driver.setVehicleId("KA01AB1234");
        driver.setVehicleType(VehicleType.SEDAN);
        driver.setVehicleVerified(true);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setCurrentLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.5946, 12.9716})));
        driver = driverRepository.save(driver);

        // 3. Create Rider User
        User riderUser = new User();
        riderUser.setName("Rider User");
        riderUser.setEmail("rider@example.com");
        riderUser.setPhoneNumber("+919876543211");
        riderUser.setPassword("password");
        riderUser.setIsVerified(true);
        riderUser.setRoles(Set.of(Role.RIDER));
        riderUser = userRepository.save(riderUser);

        // 4. Create Rider
        rider = new Rider();
        rider.setUser(riderUser);
        rider.setRating(4.8);
        rider = riderRepository.save(rider);

        // 5. Create Wallets
        Wallet driverWallet = new Wallet();
        driverWallet.setUser(driverUser);
        driverWallet.setBalance(BigDecimal.valueOf(100.00));
        walletRepository.save(driverWallet);

        Wallet riderWallet = new Wallet();
        riderWallet.setUser(riderUser);
        riderWallet.setBalance(BigDecimal.valueOf(500.00));
        walletRepository.save(riderWallet);

        // 6. Generate Token
        JWTService.TokenDetails tokenDetails = jwtService.generateAccessToken(driverUser);
        validAccessToken = tokenDetails.token();
    }

    @Test
    void testStartRide_success() {
        // Arrange
        String otp = "1234";
        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setDriver(driver);
        ride.setPickupLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.5946, 12.9716})));
        ride.setDropOffLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.6476, 12.9081})));
        ride.setOtp(otp);
        ride.setPaymentMethod(PaymentMethod.CASH);
        ride.setRideStatus(RideStatus.CONFIRMED);
        ride.setVehicleType(VehicleType.SEDAN);
        ride.setFare(BigDecimal.valueOf(150.00));
        ride = rideRepository.save(ride);

        RideStartDto startDto = new RideStartDto();
        startDto.setOtp(otp);

        // Act & Assert
        webTestClient.post()
                .uri("/drivers/startRide/" + ride.getId())
                .header("Authorization", "Bearer " + validAccessToken)
                .bodyValue(startDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rideStatus").isEqualTo(RideStatus.ONGOING.name());

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.ONGOING, updatedRide.getRideStatus());
        assertNotNull(updatedRide.getStartedAt());
    }

    @Test
    void testStartRide_invalidOtp() {
        // Arrange
        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setDriver(driver);
        ride.setPickupLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.5946, 12.9716})));
        ride.setDropOffLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.6476, 12.9081})));
        ride.setOtp("1234");
        ride.setPaymentMethod(PaymentMethod.CASH);
        ride.setRideStatus(RideStatus.CONFIRMED);
        ride.setVehicleType(VehicleType.SEDAN);
        ride.setFare(BigDecimal.valueOf(150.00));
        ride = rideRepository.save(ride);

        RideStartDto startDto = new RideStartDto();
        startDto.setOtp("9999"); // Incorrect OTP

        // Act & Assert
        webTestClient.post()
                .uri("/drivers/startRide/" + ride.getId())
                .header("Authorization", "Bearer " + validAccessToken)
                .bodyValue(startDto)
                .exchange()
                .expectStatus().is5xxServerError();

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertEquals(RideStatus.CONFIRMED, updatedRide.getRideStatus());
        assertNull(updatedRide.getStartedAt());
    }

    @Test
    void testStartRide_wrongDriver() {
        // Arrange
        // Create another driver
        User otherUser = new User();
        otherUser.setName("Other Driver");
        otherUser.setEmail("other@example.com");
        otherUser.setPhoneNumber("+919876543212");
        otherUser.setPassword("password");
        otherUser.setIsVerified(true);
        otherUser.setRoles(Set.of(Role.DRIVER));
        otherUser = userRepository.save(otherUser);

        Driver otherDriver = new Driver();
        otherDriver.setUser(otherUser);
        otherDriver.setRating(4.7);
        otherDriver.setAvailable(true);
        otherDriver.setVehicleId("KA01AB1235");
        otherDriver.setVehicleType(VehicleType.SEDAN);
        otherDriver.setVehicleVerified(true);
        otherDriver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        otherDriver.setCurrentLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.5946, 12.9716})));
        otherDriver = driverRepository.save(otherDriver);

        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setDriver(otherDriver); // Assigned to other driver
        ride.setPickupLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.5946, 12.9716})));
        ride.setDropOffLocation(GeometryUtil.createPoint(new PointDto(new double[]{77.6476, 12.9081})));
        ride.setOtp("1234");
        ride.setPaymentMethod(PaymentMethod.CASH);
        ride.setRideStatus(RideStatus.CONFIRMED);
        ride.setVehicleType(VehicleType.SEDAN);
        ride.setFare(BigDecimal.valueOf(150.00));
        ride = rideRepository.save(ride);

        RideStartDto startDto = new RideStartDto();
        startDto.setOtp("1234");

        // Act & Assert - Should fail since validAccessToken belongs to driver, but ride is assigned to otherDriver
        webTestClient.post()
                .uri("/drivers/startRide/" + ride.getId())
                .header("Authorization", "Bearer " + validAccessToken)
                .bodyValue(startDto)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}

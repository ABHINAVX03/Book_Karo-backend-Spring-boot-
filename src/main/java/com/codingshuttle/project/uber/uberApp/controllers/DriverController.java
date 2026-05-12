package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.services.AuthService;
import com.codingshuttle.project.uber.uberApp.services.DriverService;
import com.codingshuttle.project.uber.uberApp.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import com.codingshuttle.project.uber.uberApp.entities.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/drivers")
@Secured("ROLE_DRIVER")
public class DriverController {

    private final DriverService driverService;
    private final WalletService walletService;
    private final AuthService authService;

    @PostMapping("/acceptRide/{rideRequestId}")
    public ResponseEntity<RideDto> acceptRide(@PathVariable Long rideRequestId) {
        return ResponseEntity.ok(driverService.acceptRide(rideRequestId));
    }

    @PostMapping("/startRide/{rideRequestId}")
    public ResponseEntity<RideDto> startRide(@PathVariable Long rideRequestId,
                                              @RequestBody RideStartDto rideStartDto) {
        return ResponseEntity.ok(driverService.startRide(rideRequestId, rideStartDto.getOtp()));
    }

    @PostMapping("/endRide/{rideId}")
    public ResponseEntity<RideDto> endRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(driverService.endRide(rideId));
    }

    @PatchMapping("/updateLocation")
    public ResponseEntity<DriverDto> updateLocation(@RequestBody PointDto pointDto) {
        return ResponseEntity.ok(driverService.updateLocation(pointDto));
    }

    @GetMapping("/getIncomingRideRequest")
    public ResponseEntity<RideRequestDto> getIncomingRideRequest() {
        RideRequestDto rideRequest = driverService.getIncomingRideRequest();
        if (rideRequest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(rideRequest);
    }

    @PostMapping("/cancelRide/{rideId}")
    public ResponseEntity<RideDto> cancelRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(driverService.cancelRide(rideId));
    }

    @PostMapping("/rateRider")
    public ResponseEntity<RiderDto> rateRider(@RequestBody RatingDto ratingDto) {
        return ResponseEntity.ok(driverService.rateRider(ratingDto.getRideId(), ratingDto.getRating()));
    }

    @GetMapping("/getMyProfile")
    public ResponseEntity<DriverDto> getMyProfile() {
        return ResponseEntity.ok(driverService.getMyProfile());
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UpdateProfileDto updateProfileDto) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(authService.updateProfile(user.getId(), updateProfileDto));
    }

    @GetMapping("/getMyRides")
    public ResponseEntity<Page<RideDto>> getAllMyRides(@RequestParam(defaultValue = "0") Integer pageOffset,
                                                       @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize,
                Sort.by(Sort.Direction.DESC, "createdTime", "id"));
        return ResponseEntity.ok(driverService.getAllMyRides(pageRequest));
    }

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getMyWallet() {
        return ResponseEntity.ok(walletService.getMyWallet());
    }

    @PostMapping("/wallet/addMoney")
    public ResponseEntity<WalletDto> addMoneyToWallet(@RequestBody WalletAmountDto walletAmountDto) {
        return ResponseEntity.ok(walletService.addMoneyToMyWallet(walletAmountDto.getAmount()));
    }

    @PostMapping("/wallet/withdraw")
    public ResponseEntity<WalletDto> withdrawMoneyFromWallet(@RequestBody WalletAmountDto walletAmountDto) {
        return ResponseEntity.ok(walletService.withdrawMoneyFromMyWallet(walletAmountDto.getAmount()));
    }
}

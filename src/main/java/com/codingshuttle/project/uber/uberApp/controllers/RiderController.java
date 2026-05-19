package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.services.AuthService;
import com.codingshuttle.project.uber.uberApp.services.RiderService;
import com.codingshuttle.project.uber.uberApp.services.WalletService;
import com.codingshuttle.project.uber.uberApp.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/riders")
@RequiredArgsConstructor
@Secured("ROLE_RIDER")
public class RiderController {

    private final RiderService riderService;
    private final WalletService walletService;
    private final AuthService authService;

    @PostMapping("/estimateFare")
    public ResponseEntity<RideRequestDto> estimateFare(@Valid @RequestBody RideRequestDto rideRequestDto) {
        return ResponseEntity.ok(riderService.estimateFare(rideRequestDto));
    }

    @PostMapping("/requestRide")
    public ResponseEntity<RideRequestDto> requestRide(@Valid @RequestBody RideRequestDto rideRequestDto) {
        return ResponseEntity.ok(riderService.requestRide(rideRequestDto));
    }

    @PostMapping("/cancelRideRequest/{rideRequestId}")
    public ResponseEntity<RideRequestDto> cancelRideRequest(@PathVariable Long rideRequestId) {
        return ResponseEntity.ok(riderService.cancelRideRequest(rideRequestId));
    }

    @PostMapping("/cancelRide/{rideId}")
    public ResponseEntity<RideDto> cancelRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(riderService.cancelRide(rideId));
    }

    @PostMapping("/rateDriver")
    public ResponseEntity<DriverDto> rateDriver(@Valid @RequestBody RatingDto ratingDto) {
        return ResponseEntity.ok(riderService.rateDriver(ratingDto.getRideId(), ratingDto.getRating()));
    }

    @GetMapping("/getMyProfile")
    public ResponseEntity<RiderDto> getMyProfile() {
        return ResponseEntity.ok(riderService.getMyProfile());
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UpdateProfileDto updateProfileDto) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(authService.updateProfile(user.getId(), updateProfileDto));
    }

    @GetMapping("/getMyRides")
    public ResponseEntity<Page<RideDto>> getAllMyRides(
            @RequestParam(defaultValue = "0") Integer pageOffset,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(pageOffset, pageSize,
                Sort.by(Sort.Direction.DESC, "createdTime", "id"));
        return ResponseEntity.ok(riderService.getAllMyRides(pageRequest));
    }

    /**
     * NEW ENDPOINT — FIX for BUG-07.
     *
     * Returns the rider's most recent CONFIRMED or ONGOING ride.
     * This replaces the unreliable "search through first 10 rides" approach
     * in BookRidePage.jsx and ensures the rider sees their active ride
     * even if they have many past rides. Returns 204 No Content if no
     * active ride exists.
     */
    @GetMapping("/currentRide")
    public ResponseEntity<RideDto> getCurrentRide() {
        RideDto ride = riderService.getCurrentActiveRide();
        if (ride == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ride);
    }

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getMyWallet() {
        return ResponseEntity.ok(walletService.getMyWallet());
    }

    @PostMapping("/wallet/addMoney")
    public ResponseEntity<WalletPaymentOrderDto> addMoneyToWallet(@Valid @RequestBody WalletAmountDto walletAmountDto) {
        return ResponseEntity.ok(walletService.createWalletTopUpOrder(walletAmountDto.getAmount()));
    }

    @PostMapping("/wallet/payment-order")
    public ResponseEntity<WalletPaymentOrderDto> createWalletPaymentOrder(@Valid @RequestBody WalletAmountDto walletAmountDto) {
        return ResponseEntity.ok(walletService.createWalletTopUpOrder(walletAmountDto.getAmount()));
    }

    @PostMapping("/wallet/verify-payment")
    public ResponseEntity<WalletDto> verifyWalletPayment(@Valid @RequestBody WalletPaymentVerificationDto verificationDto) {
        return ResponseEntity.ok(walletService.verifyWalletTopUpPayment(verificationDto));
    }

    @PostMapping("/wallet/withdraw")
    public ResponseEntity<WalletDto> withdrawMoneyFromWallet(@Valid @RequestBody WalletAmountDto walletAmountDto) {
        return ResponseEntity.ok(walletService.withdrawMoneyFromMyWallet(walletAmountDto.getAmount()));
    }

    /** Step 1: Rider requests a Razorpay order for an ended ride */
    @PostMapping("/rides/{rideId}/payment-order")
    public ResponseEntity<RidePaymentOrderDto> createRidePaymentOrder(@PathVariable Long rideId) {
        return ResponseEntity.ok(riderService.createRidePaymentOrder(rideId));
    }

    /** Step 2: Rider verifies Razorpay payment, completing the ride settlement */
    @PostMapping("/rides/{rideId}/verify-ride-payment")
    public ResponseEntity<RideDto> verifyRidePayment(
            @PathVariable Long rideId,
            @Valid @RequestBody WalletPaymentVerificationDto dto) {
        return ResponseEntity.ok(riderService.verifyAndCompleteRidePayment(rideId, dto));
    }
}

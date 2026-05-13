package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
import com.codingshuttle.project.uber.uberApp.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface RiderService {

    RideRequestDto estimateFare(RideRequestDto rideRequestDto);
    RideRequestDto requestRide(RideRequestDto rideRequestDto);
    RideRequestDto cancelRideRequest(Long rideRequestId);
    RideDto cancelRide(Long rideId);
    DriverDto rateDriver(Long rideId, Integer rating);
    RiderDto getMyProfile();
    Page<RideDto> getAllMyRides(PageRequest pageRequest);
    Rider createNewRider(User user);
    Rider getCurrentRider();

    // Razorpay ride payment
    RidePaymentOrderDto createRidePaymentOrder(Long rideId);
    RideDto verifyAndCompleteRidePayment(Long rideId, WalletPaymentVerificationDto dto);
}
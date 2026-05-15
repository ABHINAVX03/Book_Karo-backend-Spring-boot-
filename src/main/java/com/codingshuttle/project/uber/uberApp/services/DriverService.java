package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.dto.DriverDto;
import com.codingshuttle.project.uber.uberApp.dto.RideRequestDto;
import com.codingshuttle.project.uber.uberApp.dto.PointDto;
import com.codingshuttle.project.uber.uberApp.dto.RideDto;
import com.codingshuttle.project.uber.uberApp.dto.RiderDto;
import com.codingshuttle.project.uber.uberApp.entities.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface DriverService {

    RideDto acceptRide(Long rideRequestId);

    RideDto cancelRide(Long rideId);

    RideDto startRide(Long rideId, String otp);

    RideDto endRide(Long rideId);

    RiderDto rateRider(Long rideId, Integer rating);

    DriverDto getMyProfile();

    DriverDto updateLocation(PointDto pointDto);

    RideRequestDto getIncomingRideRequest();

    Page<RideDto> getAllMyRides(PageRequest pageRequest);

    Driver getCurrentDriver();

    Driver updateDriverAvailability(Driver driver, boolean available);
    
    DriverDto updateDriverAvailability(boolean available);

    Driver createNewDriver(Driver driver);

    // --- Verification Methods ---
    String uploadDocument(org.springframework.web.multipart.MultipartFile file, String docType);

    com.codingshuttle.project.uber.uberApp.dto.DriverVerificationDto getDriverVerificationDetails(Long driverId);

    org.springframework.data.domain.Page<com.codingshuttle.project.uber.uberApp.dto.DriverVerificationDto> getPendingDrivers(org.springframework.data.domain.PageRequest pageRequest);

    void approveDriver(Long driverId);

    void rejectDriver(Long driverId, String reason);
}

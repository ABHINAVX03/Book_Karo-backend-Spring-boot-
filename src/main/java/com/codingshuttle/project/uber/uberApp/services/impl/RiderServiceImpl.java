package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.DriverDto;
import com.codingshuttle.project.uber.uberApp.dto.RideDto;
import com.codingshuttle.project.uber.uberApp.dto.RideRequestDto;
import com.codingshuttle.project.uber.uberApp.dto.RiderDto;
import com.codingshuttle.project.uber.uberApp.entities.*;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import com.codingshuttle.project.uber.uberApp.exceptions.InvalidRideStatusException;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.exceptions.UnauthorizedAccessException;
import com.codingshuttle.project.uber.uberApp.repositories.RideRequestRepository;
import com.codingshuttle.project.uber.uberApp.repositories.RiderRepository;
import com.codingshuttle.project.uber.uberApp.services.*;
import com.codingshuttle.project.uber.uberApp.strategies.RideStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderServiceImpl implements RiderService {

    private final ModelMapper modelMapper;
    private final RideStrategyManager rideStrategyManager;
    private final RideRequestRepository rideRequestRepository;
    private final RiderRepository riderRepository;
    private final RideService rideService;
    private final DriverService driverService;
    private final RatingService ratingService;
    private final NotificationService notificationService;   // <-- injected

    @Override
    public RideRequestDto estimateFare(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();

        RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);

        Double fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);

        return modelMapper.map(rideRequest, RideRequestDto.class);
    }

    /**
     * Creates a new ride request:
     * 1. Calculates fare via the configured strategy.
     * 2. Persists the request with PENDING status.
     * 3. Finds matching drivers and notifies them.
     */
    @Override
    @Transactional
    public RideRequestDto requestRide(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();

        RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);

        Double fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);

        // Generate random 4-digit OTP
        String otp = String.format("%04d", new java.util.Random().nextInt(10000));
        rideRequest.setOtp(otp);  // ADD THIS

        RideRequest savedRideRequest = rideRequestRepository.save(rideRequest);
        log.info("RideRequest id={} created for rider id={}, fare={}",
                savedRideRequest.getId(), rider.getId(), fare);

        List<Driver> matchedDrivers = rideStrategyManager
                .driverMatchingStrategy(rider.getRating())
                .findMatchingDriver(savedRideRequest);

        // Save the matched drivers on the request so acceptRide() can validate them
        savedRideRequest.setNotifiedDrivers(matchedDrivers);
        rideRequestRepository.save(savedRideRequest);

        // Notify all matched drivers about the new ride request
        notificationService.notifyDriversAboutRideRequest(savedRideRequest, matchedDrivers);

        return modelMapper.map(savedRideRequest, RideRequestDto.class);
    }

    /**
     * Cancels a CONFIRMED ride.
     * Guards:
     *  - The calling rider must own the ride.
     *  - The ride must be in CONFIRMED status.
     * Side-effects:
     *  - Ride status → CANCELLED.
     *  - Driver availability → true.
     *  - Driver is notified of the cancellation.
     */
    @Override
    @Transactional
    public RideDto cancelRide(Long rideId) {
        Rider rider = getCurrentRider();
        Ride ride = rideService.getRideById(rideId);

        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException(
                    "Rider id=" + rider.getId() + " does not own ride id=" + rideId);
        }

        if (!ride.getRideStatus().equals(RideStatus.CONFIRMED)) {
            throw new InvalidRideStatusException(
                    "Ride id=" + rideId + " cannot be cancelled; current status: " + ride.getRideStatus());
        }

        Ride savedRide = rideService.updateRideStatus(ride, RideStatus.CANCELLED);
        driverService.updateDriverAvailability(ride.getDriver(), true);

        // Notify the driver that the rider cancelled
        notificationService.notifyDriverAboutRideCancellation(ride.getDriver(), rideId);

        log.info("Ride id={} cancelled by rider id={}", rideId, rider.getId());
        return modelMapper.map(savedRide, RideDto.class);
    }

    /**
     * Rates the driver after a completed ride.
     * Guards:
     *  - The calling rider must own the ride.
     *  - The ride must be in ENDED status.
     */
    @Override
    public DriverDto rateDriver(Long rideId, Integer rating) {
        Ride ride = rideService.getRideById(rideId);
        Rider rider = getCurrentRider();

        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException(
                    "Rider id=" + rider.getId() + " is not the owner of ride id=" + rideId);
        }

        if (!ride.getRideStatus().equals(RideStatus.ENDED)) {
            throw new InvalidRideStatusException(
                    "Cannot rate driver for ride id=" + rideId +
                            "; ride must be ENDED but is: " + ride.getRideStatus());
        }

        log.info("Rider id={} rating driver for ride id={} with rating={}", rider.getId(), rideId, rating);
        return ratingService.rateDriver(ride, rating);
    }

    /**
     * Returns the profile of the currently authenticated rider.
     */
    @Override
    public RiderDto getMyProfile() {
        Rider currentRider = getCurrentRider();
        return modelMapper.map(currentRider, RiderDto.class);
    }

    /**
     * Returns a paginated list of all rides for the currently authenticated rider.
     */
    @Override
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        Rider currentRider = getCurrentRider();
        return rideService.getAllRidesOfRider(currentRider, pageRequest)
                .map(ride -> modelMapper.map(ride, RideDto.class));
    }

    /**
     * Creates and persists a new Rider entity linked to the given User.
     * Called during user registration/onboarding.
     */
    @Override
    public Rider createNewRider(User user) {
        Rider rider = Rider.builder()
                .user(user)
                .rating(0.0)
                .build();
        Rider saved = riderRepository.save(rider);
        log.info("New rider id={} created for user id={}", saved.getId(), user.getId());
        return saved;
    }

    /**
     * Resolves the currently authenticated user to their Rider entity.
     * Throws {@link ResourceNotFoundException} if no rider profile exists for the user.
     */
    @Override
    public Rider getCurrentRider() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return riderRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rider not associated with user id=" + user.getId()));
    }
}
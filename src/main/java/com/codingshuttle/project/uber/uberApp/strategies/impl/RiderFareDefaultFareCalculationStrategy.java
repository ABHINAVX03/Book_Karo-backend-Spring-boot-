package com.codingshuttle.project.uber.uberApp.strategies.impl;

import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import com.codingshuttle.project.uber.uberApp.services.DistanceService;
import com.codingshuttle.project.uber.uberApp.strategies.RideFareCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RiderFareDefaultFareCalculationStrategy implements RideFareCalculationStrategy {

    private final DistanceService distanceService;

    @Override
    public double calculateFare(RideRequest rideRequest) {
        double distance = distanceService.calculateDistance(rideRequest.getPickupLocation(),
                rideRequest.getDropOffLocation());
        
        double vehicleMultiplier = 1.0;
        if (rideRequest.getVehicleType() != null) {
            vehicleMultiplier = switch (rideRequest.getVehicleType()) {
                case SEDAN -> 1.3;
                case LUXE -> 2.0;
                default -> 1.0;
            };
        }
        
        return distance * RIDE_FARE_MULTIPLIER * vehicleMultiplier;
    }
}

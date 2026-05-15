package com.codingshuttle.project.uber.uberApp.strategies.impl;

import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import com.codingshuttle.project.uber.uberApp.services.DistanceService;
import com.codingshuttle.project.uber.uberApp.strategies.RideFareCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RideFareSurgePricingFareCalculationStrategy implements RideFareCalculationStrategy {

    private final DistanceService distanceService;
    private static final BigDecimal SURGE_FACTOR = new BigDecimal("2.0");

    @Override
    public BigDecimal calculateFare(RideRequest rideRequest) {
        double distance = distanceService.calculateDistance(rideRequest.getPickupLocation(),
                rideRequest.getDropOffLocation());
        
        BigDecimal vehicleMultiplier = BigDecimal.ONE;
        if (rideRequest.getVehicleType() != null) {
            vehicleMultiplier = switch (rideRequest.getVehicleType()) {
                case SEDAN -> new BigDecimal("1.3");
                case LUXE -> new BigDecimal("2.0");
                default -> BigDecimal.ONE;
            };
        }

        return BigDecimal.valueOf(distance)
                .multiply(RIDE_FARE_MULTIPLIER)
                .multiply(SURGE_FACTOR)
                .multiply(vehicleMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

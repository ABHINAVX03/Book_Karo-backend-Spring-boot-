package com.codingshuttle.project.uber.uberApp.strategies;

import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import java.math.BigDecimal;

public interface RideFareCalculationStrategy {

    BigDecimal RIDE_FARE_MULTIPLIER = new BigDecimal("10");

    BigDecimal calculateFare(RideRequest rideRequest);

}

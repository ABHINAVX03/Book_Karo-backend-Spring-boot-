package com.codingshuttle.project.uber.uberApp.strategies;

import com.codingshuttle.project.uber.uberApp.strategies.impl.DriverMatchingNearestDriverStrategy;
import com.codingshuttle.project.uber.uberApp.strategies.impl.RideFareSurgePricingFareCalculationStrategy;
import com.codingshuttle.project.uber.uberApp.strategies.impl.RiderFareDefaultFareCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class RideStrategyManager {

    private final DriverMatchingNearestDriverStrategy nearestDriverStrategy;
    private final RideFareSurgePricingFareCalculationStrategy surgePricingFareCalculationStrategy;
    private final RiderFareDefaultFareCalculationStrategy defaultFareCalculationStrategy;

    /**
     * Prefer nearest drivers within the configured radius for all riders.
     * (High-rating riders previously used "top rated in bubble" ordering, which could
     * exclude nearby drivers and made incoming requests invisible on the driver panel.)
     */
    public DriverMatchingStrategy driverMatchingStrategy(double riderRating) {
        return nearestDriverStrategy;
    }

    public RideFareCalculationStrategy rideFareCalculationStrategy() {

        LocalTime morningSurgeStart = LocalTime.of(8, 0);
        LocalTime morningSurgeEnd = LocalTime.of(11, 0);
        LocalTime eveningSurgeStart = LocalTime.of(18, 0);
        LocalTime eveningSurgeEnd = LocalTime.of(21, 0);
        LocalTime currentTime = LocalTime.now();

        boolean isMorningSurge = currentTime.isAfter(morningSurgeStart) && currentTime.isBefore(morningSurgeEnd);
        boolean isEveningSurge = currentTime.isAfter(eveningSurgeStart) && currentTime.isBefore(eveningSurgeEnd);

        if(isMorningSurge || isEveningSurge) {
            return surgePricingFareCalculationStrategy;
        } else {
            return defaultFareCalculationStrategy;
        }
    }

}

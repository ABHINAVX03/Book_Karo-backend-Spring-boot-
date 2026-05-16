package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.AdminRevenueDto;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import com.codingshuttle.project.uber.uberApp.repositories.RideRepository;
import com.codingshuttle.project.uber.uberApp.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RideRepository rideRepository;
    private static final BigDecimal PLATFORM_COMMISSION_PERCENTAGE = new BigDecimal("0.3");

    @Override
    public AdminRevenueDto getRevenueStats(Pageable pageable) {
        Long totalCompletedRides = rideRepository.countByRideStatus(RideStatus.ENDED);
        BigDecimal totalFareCollected = rideRepository.sumFareByRideStatus(RideStatus.ENDED);
        if (totalFareCollected == null) totalFareCollected = BigDecimal.ZERO;

        BigDecimal totalCommissionEarned = totalFareCollected.multiply(PLATFORM_COMMISSION_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDriverPayouts = totalFareCollected.subtract(totalCommissionEarned);

        Page<Ride> ridePage = rideRepository.findByRideStatus(RideStatus.ENDED, pageable);

        return AdminRevenueDto.builder()
                .totalCompletedRides(totalCompletedRides)
                .totalFareCollected(totalFareCollected)
                .totalCommissionEarned(totalCommissionEarned)
                .totalDriverPayouts(totalDriverPayouts)
                .rides(ridePage.getContent().stream()
                        .map(this::mapToRideRecordDto)
                        .collect(Collectors.toList()))
                .currentPage(ridePage.getNumber())
                .totalPages(ridePage.getTotalPages())
                .totalElements(ridePage.getTotalElements())
                .build();
    }

    private AdminRevenueDto.AdminRideRecordDto mapToRideRecordDto(Ride ride) {
        BigDecimal totalFare = ride.getFare();
        if (totalFare == null) totalFare = BigDecimal.ZERO;
        
        BigDecimal platformCommission = totalFare.multiply(PLATFORM_COMMISSION_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverPayout = totalFare.subtract(platformCommission);

        return AdminRevenueDto.AdminRideRecordDto.builder()
                .rideId(ride.getId())
                .createdTime(ride.getCreatedTime() != null ? ride.getCreatedTime().toString() : "")
                .endedAt(ride.getEndedAt() != null ? ride.getEndedAt().toString() : "")
                .riderName(ride.getRider() != null && ride.getRider().getUser() != null ? ride.getRider().getUser().getName() : "N/A")
                .driverName(ride.getDriver() != null && ride.getDriver().getUser() != null ? ride.getDriver().getUser().getName() : "N/A")
                .paymentMethod(ride.getPaymentMethod() != null ? ride.getPaymentMethod().toString() : "N/A")
                .totalFare(totalFare)
                .platformCommission(platformCommission)
                .driverPayout(driverPayout)
                .build();
    }
}

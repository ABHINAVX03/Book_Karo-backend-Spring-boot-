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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RideRepository rideRepository;
    private static final Double PLATFORM_COMMISSION_PERCENTAGE = 0.3;

    @Override
    public AdminRevenueDto getRevenueStats(Pageable pageable) {
        Long totalCompletedRides = rideRepository.countByRideStatus(RideStatus.ENDED);
        Double totalFareCollected = rideRepository.sumFareByRideStatus(RideStatus.ENDED);
        Double totalCommissionEarned = totalFareCollected * PLATFORM_COMMISSION_PERCENTAGE;
        Double totalDriverPayouts = totalFareCollected - totalCommissionEarned;

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
        Double totalFare = ride.getFare();
        Double platformCommission = totalFare * PLATFORM_COMMISSION_PERCENTAGE;
        Double driverPayout = totalFare - platformCommission;

        return AdminRevenueDto.AdminRideRecordDto.builder()
                .rideId(ride.getId())
                .createdTime(ride.getCreatedTime() != null ? ride.getCreatedTime().toString() : "")
                .endedAt(ride.getEndedAt() != null ? ride.getEndedAt().toString() : "")
                .riderName(ride.getRider().getUser().getName())
                .driverName(ride.getDriver().getUser().getName())
                .paymentMethod(ride.getPaymentMethod() != null ? ride.getPaymentMethod().toString() : "N/A")
                .totalFare(totalFare)
                .platformCommission(platformCommission)
                .driverPayout(driverPayout)
                .build();
    }
}

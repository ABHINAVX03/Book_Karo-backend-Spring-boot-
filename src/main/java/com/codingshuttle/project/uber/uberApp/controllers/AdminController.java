package com.codingshuttle.project.uber.uberApp.controllers;

import com.codingshuttle.project.uber.uberApp.dto.AdminRevenueDto;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import com.codingshuttle.project.uber.uberApp.repositories.RideRepository;
import com.codingshuttle.project.uber.uberApp.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class AdminController {

    private final RideRepository rideRepository;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @GetMapping("/revenue")
    public ResponseEntity<AdminRevenueDto> getRevenueStats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        // Summary stats across ALL completed rides
        Long totalRides = rideRepository.countByRideStatus(RideStatus.ENDED);
        Double totalFare = rideRepository.sumFareByRideStatus(RideStatus.ENDED);
        if (totalFare == null) totalFare = 0.0;

        double commission = totalFare * PaymentStrategy.PLATFORM_COMMISSION;
        double driverPayouts = totalFare * (1 - PaymentStrategy.PLATFORM_COMMISSION);

        // Paginated ride records
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "endedAt", "id"));
        Page<Ride> ridePage = rideRepository.findByRideStatus(RideStatus.ENDED, pageRequest);

        List<AdminRevenueDto.AdminRideRecordDto> records = ridePage.getContent().stream()
                .map(ride -> {
                    double fare = ride.getFare() != null ? ride.getFare() : 0.0;
                    return AdminRevenueDto.AdminRideRecordDto.builder()
                            .rideId(ride.getId())
                            .createdTime(ride.getCreatedTime() != null
                                    ? ride.getCreatedTime().format(FORMATTER) : null)
                            .endedAt(ride.getEndedAt() != null
                                    ? ride.getEndedAt().format(FORMATTER) : null)
                            .riderName(ride.getRider() != null && ride.getRider().getUser() != null
                                    ? ride.getRider().getUser().getName() : "N/A")
                            .driverName(ride.getDriver() != null && ride.getDriver().getUser() != null
                                    ? ride.getDriver().getUser().getName() : "N/A")
                            .paymentMethod(ride.getPaymentMethod() != null
                                    ? ride.getPaymentMethod().name() : "N/A")
                            .totalFare(fare)
                            .platformCommission(Math.round(fare * PaymentStrategy.PLATFORM_COMMISSION * 100.0) / 100.0)
                            .driverPayout(Math.round(fare * (1 - PaymentStrategy.PLATFORM_COMMISSION) * 100.0) / 100.0)
                            .build();
                })
                .toList();

        AdminRevenueDto response = AdminRevenueDto.builder()
                .totalCompletedRides(totalRides)
                .totalFareCollected(Math.round(totalFare * 100.0) / 100.0)
                .totalCommissionEarned(Math.round(commission * 100.0) / 100.0)
                .totalDriverPayouts(Math.round(driverPayouts * 100.0) / 100.0)
                .rides(records)
                .currentPage(ridePage.getNumber())
                .totalPages(ridePage.getTotalPages())
                .totalElements(ridePage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }
}
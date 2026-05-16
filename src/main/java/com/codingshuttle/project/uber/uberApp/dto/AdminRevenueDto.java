package com.codingshuttle.project.uber.uberApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueDto {

    // Summary stats
    private Long totalCompletedRides;
    private BigDecimal totalFareCollected;
    private BigDecimal totalCommissionEarned;
    private BigDecimal totalDriverPayouts;

    // Paginated ride records
    private List<AdminRideRecordDto> rides;
    private int currentPage;
    private int totalPages;
    private long totalElements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminRideRecordDto {
        private Long rideId;
        private String createdTime;
        private String endedAt;
        private String riderName;
        private String driverName;
        private String paymentMethod;
        private BigDecimal totalFare;
        private BigDecimal platformCommission;
        private BigDecimal driverPayout;
    }
}
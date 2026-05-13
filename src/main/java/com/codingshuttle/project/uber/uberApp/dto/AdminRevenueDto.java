package com.codingshuttle.project.uber.uberApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueDto {

    // Summary stats
    private Long totalCompletedRides;
    private Double totalFareCollected;
    private Double totalCommissionEarned;
    private Double totalDriverPayouts;

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
        private Double totalFare;
        private Double platformCommission;
        private Double driverPayout;
    }
}
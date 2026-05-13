package com.codingshuttle.project.uber.uberApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RidePaymentOrderDto {
    private String key;        // Razorpay key ID (for frontend checkout)
    private String orderId;    // Razorpay order ID
    private Integer amount;    // Amount in paise
    private String currency;
    private String name;       // Merchant display name
    private Long rideId;
    private String description;
}
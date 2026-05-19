package com.codingshuttle.project.uber.uberApp.dto;

import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestDto {

    private Long id;

    @Valid
    @NotNull(message = "Pickup location is required")
    private PointDto pickupLocation;

    @Valid
    @NotNull(message = "Dropoff location is required")
    private PointDto dropOffLocation;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private LocalDateTime requestedTime;

    private RiderDto rider;
    private BigDecimal fare;

    private RideRequestStatus rideRequestStatus;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    private String otp;
}

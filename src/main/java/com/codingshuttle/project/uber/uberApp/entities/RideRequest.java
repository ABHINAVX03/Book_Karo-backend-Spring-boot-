package com.codingshuttle.project.uber.uberApp.entities;

import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
    indexes = {
        @Index(name = "idx_ride_request_rider", columnList = "rider_id"),
        @Index(name = "idx_ride_request_status", columnList = "rideRequestStatus")
    }
)
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "Geometry(Point, 4326)")
    private Point pickupLocation;

    @Column(columnDefinition = "Geometry(Point, 4326)")
    private Point dropOffLocation;

    @CreationTimestamp
    private LocalDateTime requestedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private Rider rider;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private RideRequestStatus rideRequestStatus;
    
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private BigDecimal fare;

    private String otp;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ride_request_notified_drivers",
            joinColumns = @JoinColumn(name = "ride_request_id"),
            inverseJoinColumns = @JoinColumn(name = "driver_id")
    )
    private List<Driver> notifiedDrivers = new ArrayList<>();

    /**
     * Optimistic locking version — prevents two drivers from accepting
     * the same ride request in a race condition.
     */
    @Version
    private Long version;
}

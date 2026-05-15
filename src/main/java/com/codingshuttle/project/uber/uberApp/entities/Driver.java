package com.codingshuttle.project.uber.uberApp.entities;

import com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_driver_vehicle_id", columnList = "vehicleId"),
        @Index(name = "idx_driver_available_type", columnList = "available, vehicleType")
})
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Double rating;

    private Boolean available;

    private String vehicleId;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    // --- Verification Fields ---
    private String vehicleModel;
    
    private Boolean vehicleVerified = false;

    @Enumerated(EnumType.STRING)
    private DriverVerificationStatus verificationStatus = DriverVerificationStatus.PENDING;

    private String rejectionReason;

    private String rcUrl;
    private String licenseUrl;
    private String insuranceUrl;
    private String profilePhotoUrl;

    private Boolean blocked = false;

    @Column(columnDefinition = "Geometry(Point,4326)")
    private Point currentLocation;
}

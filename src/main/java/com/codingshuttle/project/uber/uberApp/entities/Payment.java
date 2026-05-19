package com.codingshuttle.project.uber.uberApp.entities;

import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(indexes = {
        @Index(name = "idx_payment_ride", columnList = "ride_id", unique = true),
        @Index(name = "idx_payment_provider_reference", columnList = "providerPaymentId")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @OneToOne(fetch = FetchType.LAZY)
    private Ride ride;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(length = 16)
    private String currency;

    private String providerOrderId;

    private String providerPaymentId;

    private String providerSignature;

    @Column(length = 128)
    private String settlementReference;

    @CreationTimestamp
    private LocalDateTime paymentTime;

    private LocalDateTime processedAt;
}

package com.codingshuttle.project.uber.uberApp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp_challenge", indexes = {
        @Index(name = "idx_otp_challenge_phone", columnList = "phoneNumber", unique = true),
        @Index(name = "idx_otp_challenge_expires_at", columnList = "expiresAt")
})
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String phoneNumber;

    @Column(nullable = false, length = 128)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedUntil;

    private LocalDateTime blockedUntil;

    private Integer sendCount;

    private Integer failedAttempts;

    private LocalDateTime sendWindowStartedAt;
}

package com.codingshuttle.project.uber.uberApp.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auth_session", indexes = {
        @Index(name = "idx_auth_session_user", columnList = "user_id"),
        @Index(name = "idx_auth_session_jti", columnList = "tokenJti", unique = true),
        @Index(name = "idx_auth_session_expires_at", columnList = "expiresAt")
})
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenJti;

    @Column(nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime lastUsedAt;

    private String replacedByTokenJti;

    @Column(length = 128)
    private String createdByIp;

    @Column(length = 512)
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

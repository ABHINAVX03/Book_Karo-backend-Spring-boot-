package com.codingshuttle.project.uber.uberApp.security;

import com.codingshuttle.project.uber.uberApp.configs.AppSecurityProperties;
import com.codingshuttle.project.uber.uberApp.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JWTService {

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final AppSecurityProperties appSecurityProperties;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(appSecurityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenDetails generateAccessToken(User user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, appSecurityProperties.getAccessTokenMinutes() * 60);
    }

    public TokenDetails generateRefreshToken(User user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, appSecurityProperties.getRefreshTokenDays() * 24 * 60 * 60);
    }

    private TokenDetails generateToken(User user, String tokenType, long ttlSeconds) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = Map.of(
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "type", tokenType
        );

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claims(claims)
                .issuer(appSecurityProperties.getJwtIssuer())
                .audience().add(appSecurityProperties.getJwtAudience()).and()
                .id(jti)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(getSecretKey())
                .compact();

        return new TokenDetails(token, jti, issuedAt, expiresAt, tokenType);
    }

    public ParsedToken parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .requireIssuer(appSecurityProperties.getJwtIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        boolean audienceValid = claims.getAudience() != null
                && claims.getAudience().contains(appSecurityProperties.getJwtAudience());
        if (!audienceValid) {
            throw new IllegalArgumentException("Token audience is invalid.");
        }

        return new ParsedToken(
                Long.valueOf(claims.getSubject()),
                claims.getId(),
                claims.get("type", String.class),
                claims,
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }

    public Long getUserIdFromToken(String token) {
        return parseToken(token).userId();
    }

    public record TokenDetails(String token, String jti, Instant issuedAt, Instant expiresAt, String type) {
    }

    public record ParsedToken(Long userId, String jti, String type, Claims claims, Instant issuedAt, Instant expiresAt) {
    }
}

package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.entities.AuthSession;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.repositories.AuthSessionRepository;
import com.codingshuttle.project.uber.uberApp.security.JWTService;
import com.codingshuttle.project.uber.uberApp.security.TokenHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final TokenHashService tokenHashService;

    @Transactional
    public AuthSession createSession(User user, JWTService.TokenDetails refreshToken, String clientIp, String userAgent) {
        purgeExpiredSessions();
        AuthSession session = AuthSession.builder()
                .user(user)
                .tokenJti(refreshToken.jti())
                .tokenHash(tokenHashService.hash(refreshToken.token()))
                .expiresAt(LocalDateTime.ofInstant(refreshToken.expiresAt(), ZoneId.systemDefault()))
                .createdByIp(clientIp)
                .userAgent(userAgent)
                .lastUsedAt(LocalDateTime.now())
                .build();
        return authSessionRepository.save(session);
    }

    @Transactional
    public AuthSession validateRefreshSession(JWTService.ParsedToken parsedToken, String rawRefreshToken) {
        if (!JWTService.REFRESH_TOKEN_TYPE.equals(parsedToken.type())) {
            throw new IllegalArgumentException("Refresh token type is invalid.");
        }

        AuthSession session = authSessionRepository.findByTokenJti(parsedToken.jti())
                .orElseThrow(() -> new IllegalArgumentException("Refresh session was not found."));

        if (session.getRevokedAt() != null) {
            throw new IllegalArgumentException("Refresh session has already been revoked.");
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh session has expired.");
        }

        if (!tokenHashService.hash(rawRefreshToken).equals(session.getTokenHash())) {
            session.setRevokedAt(LocalDateTime.now());
            authSessionRepository.save(session);
            throw new IllegalArgumentException("Refresh token hash mismatch.");
        }

        session.setLastUsedAt(LocalDateTime.now());
        return authSessionRepository.save(session);
    }

    @Transactional
    public void revoke(AuthSession session, String replacementJti) {
        session.setRevokedAt(LocalDateTime.now());
        session.setReplacedByTokenJti(replacementJti);
        authSessionRepository.save(session);
    }

    @Transactional
    public void revokeByParsedToken(JWTService.ParsedToken parsedToken) {
        authSessionRepository.findByTokenJti(parsedToken.jti()).ifPresent(session -> {
            session.setRevokedAt(LocalDateTime.now());
            authSessionRepository.save(session);
        });
    }

    @Transactional
    public void purgeExpiredSessions() {
        authSessionRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));
    }
}

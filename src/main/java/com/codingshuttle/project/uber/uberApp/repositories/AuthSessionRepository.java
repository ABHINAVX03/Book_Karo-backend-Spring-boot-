package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.AuthSession;
import com.codingshuttle.project.uber.uberApp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByTokenJti(String tokenJti);

    void deleteByExpiresAtBefore(LocalDateTime threshold);

    long deleteByUserAndRevokedAtIsNotNullAndExpiresAtBefore(User user, LocalDateTime threshold);
}

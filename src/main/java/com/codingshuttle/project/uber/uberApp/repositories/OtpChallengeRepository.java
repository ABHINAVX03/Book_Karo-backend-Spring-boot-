package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findByPhoneNumber(String phoneNumber);

    void deleteByVerifiedUntilBeforeAndExpiresAtBefore(LocalDateTime verifiedThreshold, LocalDateTime expiryThreshold);
}

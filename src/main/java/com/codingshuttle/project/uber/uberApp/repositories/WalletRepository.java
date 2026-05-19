package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user = :user")
    Optional<Wallet> findByUserForUpdate(@Param("user") User user);
}

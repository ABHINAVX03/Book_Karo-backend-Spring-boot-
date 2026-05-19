package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Payment;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRide(Ride ride);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.ride = :ride")
    Optional<Payment> findByRideForUpdate(@Param("ride") Ride ride);
}

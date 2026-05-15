package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RideRequest r WHERE r.id = :id")
    Optional<RideRequest> findByIdWithLock(@Param("id") Long id);

    @Query("""
        SELECT rr FROM RideRequest rr
        JOIN rr.notifiedDrivers d
        WHERE d = :driver
        AND rr.rideRequestStatus = :status
        ORDER BY rr.id DESC
    """)
    List<RideRequest> findByNotifiedDriversContainingAndRideRequestStatus(
            @Param("driver") Driver driver,
            @Param("status") RideRequestStatus status
    );
}

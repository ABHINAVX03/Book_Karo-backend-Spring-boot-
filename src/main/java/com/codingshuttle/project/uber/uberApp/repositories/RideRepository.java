package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    Page<Ride> findByRider(Rider rider, Pageable pageRequest);
    Page<Ride> findByDriver(Driver driver, Pageable pageRequest);
    Page<Ride> findByRideStatus(RideStatus rideStatus, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.rideStatus = :status")
    Long countByRideStatus(RideStatus status);

    @Query("SELECT COALESCE(SUM(r.fare), 0) FROM Ride r WHERE r.rideStatus = :status")
    Double sumFareByRideStatus(RideStatus status);
}
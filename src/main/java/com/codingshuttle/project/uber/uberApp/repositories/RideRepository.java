package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    Page<Ride> findByRider(Rider rider, Pageable pageable);

    Page<Ride> findByDriver(Driver driver, Pageable pageable);

    /**
     * NEW — for the GET /riders/currentRide endpoint.
     *
     * Returns the most recent CONFIRMED or ONGOING ride for a rider.
     * This is used by BookRidePage to reliably find the accepted ride
     * without paging through all historical rides.
     */
    Optional<Ride> findTopByRiderAndRideStatusInOrderByIdDesc(Rider rider, List<RideStatus> statuses);
}

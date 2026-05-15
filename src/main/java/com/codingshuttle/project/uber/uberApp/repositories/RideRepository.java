package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // ── Original methods (must not be removed) ────────────────────────────────

    Page<Ride> findByRider(Rider rider, Pageable pageable);

    Page<Ride> findByDriver(Driver driver, Pageable pageable);

    /** Used by AdminController.getRevenueStats() — paginated ENDED rides */
    Page<Ride> findByRideStatus(RideStatus rideStatus, Pageable pageable);

    /** Used by AdminController.getRevenueStats() — total completed ride count */
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.rideStatus = :status")
    Long countByRideStatus(RideStatus status);

    /** Used by AdminController.getRevenueStats() — total fare collected */
    @Query("SELECT COALESCE(SUM(r.fare), 0) FROM Ride r WHERE r.rideStatus = :status")
    BigDecimal sumFareByRideStatus(RideStatus status);

    // ── New method added by bug-fix (BUG-07) ─────────────────────────────────

    /**
     * Returns the most recent CONFIRMED or ONGOING ride for a rider.
     * Used by RideServiceImpl.getCurrentActiveRideForRider()
     * → RiderServiceImpl.getCurrentActiveRide()
     * → GET /riders/currentRide
     *
     * Replaces the unreliable "search through first 10 rides by
     * coordinate matching" approach in BookRidePage.jsx.
     */
    Optional<Ride> findTopByRiderAndRideStatusInOrderByIdDesc(Rider rider, List<RideStatus> statuses);
}

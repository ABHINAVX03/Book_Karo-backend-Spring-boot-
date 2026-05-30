package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query(value = """
        SELECT d.*
        FROM driver d
        WHERE d.available = true
          AND d.current_location IS NOT NULL
          AND ST_DWithin(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                10000
          )
        ORDER BY ST_Distance(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            )
        LIMIT 10
    """, nativeQuery = true)
    List<Driver> findTenNearestDrivers(@Param("longitude") double longitude,
                                       @Param("latitude") double latitude);

    @Query(value = """
        SELECT d.*
        FROM driver d
        WHERE d.available = true
          AND d.vehicle_type = :vehicleType
          AND d.current_location IS NOT NULL
          AND ST_DWithin(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                10000
          )
        ORDER BY ST_Distance(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            )
        LIMIT 10
    """, nativeQuery = true)
    List<Driver> findTenNearestDriversWithVehicleType(@Param("longitude") double longitude,
                                                     @Param("latitude") double latitude,
                                                     @Param("vehicleType") String vehicleType);

    @Query(value = """
        SELECT d.*
        FROM driver d
        WHERE d.available = true
          AND d.current_location IS NOT NULL
          AND ST_DWithin(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                10000
          )
        ORDER BY ST_Distance(
                d.current_location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            ) ASC,
            d.rating DESC NULLS LAST
        LIMIT 10
    """, nativeQuery = true)
    List<Driver> findTenNearbyTopRatedDrivers(@Param("longitude") double longitude,
                                               @Param("latitude") double latitude);

    Optional<Driver> findByUser(User user);

    Page<Driver> findByVerificationStatus(DriverVerificationStatus status, Pageable pageable);

    Page<Driver> findByVerificationStatusAndVerificationSubmittedTrue(
            DriverVerificationStatus status,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT d FROM Driver d
                    JOIN FETCH d.user
                    WHERE d.verificationStatus = :status
                      AND d.verificationSubmitted = true
                    """,
            countQuery = """
                    SELECT COUNT(d) FROM Driver d
                    WHERE d.verificationStatus = :status
                      AND d.verificationSubmitted = true
                    """
    )
    Page<Driver> findSubmittedByStatusWithUser(
            @Param("status") DriverVerificationStatus status,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT d FROM Driver d
                    JOIN FETCH d.user
                    WHERE d.verificationStatus = :status
                    """,
            countQuery = "SELECT COUNT(d) FROM Driver d WHERE d.verificationStatus = :status"
    )
    Page<Driver> findByStatusWithUser(
            @Param("status") DriverVerificationStatus status,
            Pageable pageable);

    @Query("SELECT d FROM Driver d JOIN FETCH d.user WHERE d.id = :id")
    Optional<Driver> findByIdWithUser(@Param("id") Long id);
}

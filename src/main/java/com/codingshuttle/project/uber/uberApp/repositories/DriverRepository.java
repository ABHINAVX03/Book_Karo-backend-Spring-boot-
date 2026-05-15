package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.User;
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

    org.springframework.data.domain.Page<Driver> findByVerificationStatus(com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus status, org.springframework.data.domain.Pageable pageable);
}

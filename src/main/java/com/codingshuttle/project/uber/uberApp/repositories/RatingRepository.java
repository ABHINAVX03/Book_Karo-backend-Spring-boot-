package com.codingshuttle.project.uber.uberApp.repositories;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Rating;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRider(Rider rider);
    List<Rating> findByDriver(Driver driver);

    Optional<Rating> findByRide(Ride ride);

    @Query("select avg(r.driverRating) from Rating r where r.driver = :driver and r.driverRating is not null")
    Double findAverageDriverRating(Driver driver);

    @Query("select avg(r.riderRating) from Rating r where r.rider = :rider and r.riderRating is not null")
    Double findAverageRiderRating(Rider rider);
}

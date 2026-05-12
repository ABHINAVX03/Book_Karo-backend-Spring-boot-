package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.RideRequest;

import java.util.List;

public interface NotificationService {

    /**
     * Sends a ride request notification to all matched drivers.
     */
    void notifyDriversAboutRideRequest(RideRequest rideRequest, List<Driver> drivers);

    /**
     * Notifies a rider that their ride has been accepted by a driver.
     */
    void notifyRiderAboutRideAccepted(RideRequest rideRequest, Driver driver);

    /**
     * Notifies the driver that the rider has cancelled the ride.
     */
    void notifyDriverAboutRideCancellation(Driver driver, Long rideId);
}
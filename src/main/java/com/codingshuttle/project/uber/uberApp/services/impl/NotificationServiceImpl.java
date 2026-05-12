package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import com.codingshuttle.project.uber.uberApp.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub implementation — replace the log statements with your real
 * notification provider (FCM push, WebSocket, SMS, etc.).
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyDriversAboutRideRequest(RideRequest rideRequest, List<Driver> drivers) {
        if (drivers == null || drivers.isEmpty()) {
            log.warn("[Notification] No drivers to notify for RideRequest id={}", rideRequest.getId());
            return;
        }
        drivers.forEach(driver ->
                log.info("[Notification] Notifying driver id={} (user={}) about RideRequest id={}",
                        driver.getId(),
                        driver.getUser().getId(),
                        rideRequest.getId())
        );
        // TODO: replace with real push/WebSocket/SMS call, e.g.
        // drivers.forEach(driver ->
        //     fcmService.sendPush(driver.getUser().getDeviceToken(), buildPayload(rideRequest))
        // );
    }

    @Override
    public void notifyRiderAboutRideAccepted(RideRequest rideRequest, Driver driver) {
        log.info("[Notification] Notifying rider id={} that driver id={} accepted RideRequest id={}",
                rideRequest.getRider().getId(),
                driver.getId(),
                rideRequest.getId());
    }

    @Override
    public void notifyDriverAboutRideCancellation(Driver driver, Long rideId) {
        log.info("[Notification] Notifying driver id={} that ride id={} was cancelled by rider",
                driver.getId(), rideId);
    }
}
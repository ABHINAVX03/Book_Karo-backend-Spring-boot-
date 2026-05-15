package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.RideRequest;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.repositories.DriverRepository;
import com.codingshuttle.project.uber.uberApp.repositories.RideRequestRepository;
import com.codingshuttle.project.uber.uberApp.services.*;
import com.codingshuttle.project.uber.uberApp.utils.GeometryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final RideRequestService rideRequestService;
    private final RideRequestRepository rideRequestRepository;
    private final DriverRepository driverRepository;
    private final RideService rideService;
    private final ModelMapper modelMapper;
    private final PaymentService paymentService;
    private final RatingService ratingService;
    private final EmailSenderService emailSenderService;
    private final CloudinaryService cloudinaryService;

    private static final DateTimeFormatter RECEIPT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    @Transactional
    public RideDto acceptRide(Long rideRequestId) {
        RideRequest rideRequest = rideRequestService.findRideRequestById(rideRequestId);

        if (!rideRequest.getRideRequestStatus().equals(RideRequestStatus.PENDING)) {
            throw new RuntimeException("RideRequest cannot be accepted, status is " + rideRequest.getRideRequestStatus());
        }

        Driver currentDriver = getCurrentDriver();
        
        // --- Verification Restriction ---
        if (Boolean.FALSE.equals(currentDriver.getVehicleVerified())) {
            throw new RuntimeException("Vehicle verification pending. You cannot accept rides until approved.");
        }

        if (!currentDriver.getAvailable()) {
            throw new RuntimeException("Driver cannot accept ride due to unavailability");
        }

        boolean isNotifiedDriver = rideRequest.getNotifiedDrivers()
                .stream()
                .anyMatch(d -> d.getId().equals(currentDriver.getId()));

        if (!isNotifiedDriver) {
            throw new RuntimeException("Driver is not in the area for this ride request");
        }

        Driver savedDriver = updateDriverAvailability(currentDriver, false);
        Ride ride = rideService.createNewRide(rideRequest, savedDriver);
        return modelMapper.map(ride, RideDto.class);
    }

    @Override
    public RideDto cancelRide(Long rideId) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();

        if (!driver.equals(ride.getDriver())) {
            throw new RuntimeException("Driver cannot cancel a ride they did not accept");
        }
        if (!ride.getRideStatus().equals(RideStatus.CONFIRMED)) {
            throw new RuntimeException("Ride cannot be cancelled, invalid status: " + ride.getRideStatus());
        }

        rideService.updateRideStatus(ride, RideStatus.CANCELLED);
        updateDriverAvailability(driver, true);

        // Notify rider by email
        try {
            User riderUser = ride.getRider().getUser();
            String driverName = driver.getUser() != null ? driver.getUser().getName() : "your driver";
            emailSenderService.sendEmail(
                    riderUser.getEmail(),
                    "BookCar – Your ride #" + rideId + " was cancelled",
                    String.format(
                        "Hi %s,\n\nUnfortunately %s had to cancel your BookCar ride #%d.\n\nPlease rebook at your convenience — we will find you another driver right away.\n\nSorry for the inconvenience!\nBookCar Team",
                        riderUser.getName(), driverName, rideId
                    )
            );
        } catch (Exception e) {
            log.warn("Could not send cancellation email for ride id={}: {}", rideId, e.getMessage());
        }

        return modelMapper.map(ride, RideDto.class);
    }

    @Override
    public RideDto startRide(Long rideId, String otp) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();

        if (!driver.equals(ride.getDriver())) {
            throw new RuntimeException("Driver cannot start a ride they did not accept");
        }
        if (!ride.getRideStatus().equals(RideStatus.CONFIRMED)) {
            throw new RuntimeException("Ride status is not CONFIRMED hence cannot be started, status: " + ride.getRideStatus());
        }
        if (!otp.equals(ride.getOtp())) {
            throw new RuntimeException("OTP is not valid, otp: " + otp);
        }

        ride.setStartedAt(LocalDateTime.now());
        Ride savedRide = rideService.updateRideStatus(ride, RideStatus.ONGOING);

        paymentService.createNewPayment(savedRide);
        ratingService.createNewRating(savedRide);

        return modelMapper.map(savedRide, RideDto.class);
    }

    @Override
    @Transactional
    public RideDto endRide(Long rideId) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();

        if (!driver.equals(ride.getDriver())) {
            throw new RuntimeException("Driver cannot end a ride they did not accept");
        }
        if (!ride.getRideStatus().equals(RideStatus.ONGOING)) {
            throw new RuntimeException("Ride status is not ONGOING hence cannot be ended, status: " + ride.getRideStatus());
        }

        ride.setEndedAt(LocalDateTime.now());

        // Process payment BEFORE updating ride status to ensure atomicity
        // If payment fails, the ride status remains ONGOING and no inconsistent state is created
        if (PaymentMethod.RAZORPAY.equals(ride.getPaymentMethod())) {
            // For Razorpay rides: do NOT process payment here.
            // The rider will pay via Razorpay checkout and call /riders/rides/{id}/verify-ride-payment
            log.info("Ride id={} ended with RAZORPAY — awaiting rider payment", rideId);
        } else {
            // WALLET / CASH: process payment immediately (before status change)
            paymentService.processPayment(ride);
        }

        // Update ride status to ENDED only after successful payment processing
        Ride savedRide = rideService.updateRideStatus(ride, RideStatus.ENDED);
        updateDriverAvailability(driver, true);

        if (!PaymentMethod.RAZORPAY.equals(ride.getPaymentMethod())) {
            sendRideReceiptEmail(savedRide);
        }

        return modelMapper.map(savedRide, RideDto.class);
    }

    @Override
    public RiderDto rateRider(Long rideId, Integer rating) {
        Ride ride = rideService.getRideById(rideId);
        Driver driver = getCurrentDriver();

        if (!driver.equals(ride.getDriver())) {
            throw new RuntimeException("Driver is not the owner of this Ride");
        }
        if (!ride.getRideStatus().equals(RideStatus.ENDED)) {
            throw new RuntimeException("Ride status is not Ended hence cannot start rating, status: " + ride.getRideStatus());
        }

        return ratingService.rateRider(ride, rating);
    }

    @Override
    public DriverDto getMyProfile() {
        return modelMapper.map(getCurrentDriver(), DriverDto.class);
    }

    @Override
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        Driver currentDriver = getCurrentDriver();
        return rideService.getAllRidesOfDriver(currentDriver, pageRequest).map(ride -> {
            RideDto rideDto = modelMapper.map(ride, RideDto.class);
            rideDto.setRiderRating(ratingService.getRiderRating(ride));
            return rideDto;
        });
    }

    @Override
    public DriverDto updateLocation(PointDto pointDto) {
        Driver driver = getCurrentDriver();
        Point point = GeometryUtil.createPoint(pointDto);
        driver.setCurrentLocation(point);
        return modelMapper.map(driverRepository.save(driver), DriverDto.class);
    }

    @Override
    public RideRequestDto getIncomingRideRequest() {
        Driver currentDriver = getCurrentDriver();
        List<RideRequest> requests = rideRequestRepository
                .findByNotifiedDriversContainingAndRideRequestStatus(currentDriver, RideRequestStatus.PENDING);
        if (requests == null || requests.isEmpty()) return null;
        return modelMapper.map(requests.get(0), RideRequestDto.class);
    }

    @Override
    public Driver getCurrentDriver() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver not associated with user id=" + user.getId()));
    }

    @Override
    public Driver updateDriverAvailability(Driver driver, boolean available) {
        if (available && Boolean.FALSE.equals(driver.getVehicleVerified())) {
            throw new RuntimeException("Vehicle verification pending. You cannot go online.");
        }
        driver.setAvailable(available);
        return driverRepository.save(driver);
    }

    @Override
    public Driver createNewDriver(Driver driver) {
        driver.setVerificationStatus(com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus.PENDING);
        driver.setVehicleVerified(false);
        return driverRepository.save(driver);
    }

    // --- Verification Methods Implementation ---

    @Override
    public String uploadDocument(org.springframework.web.multipart.MultipartFile file, String docType) {
        Driver driver = getCurrentDriver();
        String url = cloudinaryService.uploadFile(file, "drivers/" + driver.getId());

        switch (docType.toLowerCase()) {
            case "rc" -> driver.setRcUrl(url);
            case "license" -> driver.setLicenseUrl(url);
            case "insurance" -> driver.setInsuranceUrl(url);
            case "profile", "profile-photo" -> driver.setProfilePhotoUrl(url);
            default -> throw new RuntimeException("Invalid document type: " + docType);
        }
        
        driver.setVerificationStatus(com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus.PENDING);
        driverRepository.save(driver);
        return url;
    }

    @Override
    public DriverVerificationDto getDriverVerificationDetails(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        return modelMapper.map(driver, DriverVerificationDto.class);
    }

    @Override
    public Page<DriverVerificationDto> getPendingDrivers(PageRequest pageRequest) {
        return driverRepository.findByVerificationStatus(
                com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus.PENDING, 
                pageRequest
        ).map(driver -> modelMapper.map(driver, DriverVerificationDto.class));
    }

    @Override
    @Transactional
    public void approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        
        driver.setVehicleVerified(true);
        driver.setVerificationStatus(com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus.APPROVED);
        driver.setRejectionReason(null);
        driverRepository.save(driver);
        
        log.info("Driver id={} approved by admin", driverId);
    }

    @Override
    @Transactional
    public void rejectDriver(Long driverId, String reason) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        
        driver.setVehicleVerified(false);
        driver.setVerificationStatus(com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus.REJECTED);
        driver.setRejectionReason(reason);
        driverRepository.save(driver);
        
        log.info("Driver id={} rejected by admin. Reason: {}", driverId, reason);
    }

    // ─── Receipt Email ────────────────────────────────────────────────────────
    private void sendRideReceiptEmail(Ride ride) {
        try {
            User riderUser = ride.getRider().getUser();
            String driverName = ride.getDriver() != null && ride.getDriver().getUser() != null
                    ? ride.getDriver().getUser().getName() : "N/A";
            String endedAt = ride.getEndedAt() != null ? ride.getEndedAt().format(RECEIPT_FMT) : "N/A";

            String body = String.format(
                """
                BookCar - Ride Receipt
                ══════════════════════════════
                
                Ride ID   : #%d
                Date      : %s
                Driver    : %s
                Payment   : %s
                
                ── Payment Summary ──
                Total Fare: ₹%.2f
                Status    : PAID ✓
                
                Thank you for riding with BookCar!
                """,
                ride.getId(), endedAt, driverName,
                ride.getPaymentMethod(), ride.getFare()
            );

            emailSenderService.sendEmail(
                    riderUser.getEmail(),
                    "BookCar Ride Receipt – #" + ride.getId(),
                    body
            );
        } catch (Exception e) {
            log.warn("Could not send receipt email for ride id={}: {}", ride.getId(), e.getMessage());
        }
    }
}
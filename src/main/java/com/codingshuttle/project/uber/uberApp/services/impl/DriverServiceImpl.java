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
import com.codingshuttle.project.uber.uberApp.configs.AppAdminProperties;
import com.codingshuttle.project.uber.uberApp.entities.enums.DriverVerificationStatus;
import com.codingshuttle.project.uber.uberApp.services.*;
import com.codingshuttle.project.uber.uberApp.mappers.DriverVerificationMapper;
import com.codingshuttle.project.uber.uberApp.services.storage.ResilientDocumentStorageService;
import com.codingshuttle.project.uber.uberApp.utils.GeometryUtil;
import org.springframework.security.core.GrantedAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ResilientDocumentStorageService documentStorageService;
    private final AppAdminProperties appAdminProperties;
    private final DriverVerificationMapper driverVerificationMapper;

    private static final DateTimeFormatter RECEIPT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    @Transactional
    public RideDto acceptRide(Long rideRequestId) {
        // Use pessimistic lock to prevent multiple drivers from accepting the same ride
        RideRequest rideRequest = rideRequestRepository.findByIdWithLock(rideRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest not found with id: " + rideRequestId));

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
        return toDriverRideDto(ride);
    }

    @Override
    @Transactional
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

        return toDriverRideDto(ride);
    }

    @Override
    @Transactional
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

        return toDriverRideDto(savedRide);
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

        // Process payment before status change; if payment fails unexpectedly, driver is still freed
        if (PaymentMethod.RAZORPAY.equals(ride.getPaymentMethod())) {
            log.info("Ride id={} ended with RAZORPAY — awaiting rider payment", rideId);
        } else {
            try {
                paymentService.processPayment(ride);
            } catch (Exception e) {
                log.error("Payment processing error for ride id={}: {}", rideId, e.getMessage(), e);
            }
        }

        // Update ride status to ENDED and free the driver
        Ride savedRide = rideService.updateRideStatus(ride, RideStatus.ENDED);
        updateDriverAvailability(driver, true);

        if (!PaymentMethod.RAZORPAY.equals(ride.getPaymentMethod())) {
            sendRideReceiptEmail(savedRide);
        }

        return toDriverRideDto(savedRide);
    }

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public DriverDto getMyProfile() {
        return modelMapper.map(getCurrentDriver(), DriverDto.class);
    }

    private RideDto toDriverRideDto(Ride ride) {
        if (ride == null) return null;
        RideDto rideDto = modelMapper.map(ride, RideDto.class);
        rideDto.setOtp(null); // Never leak OTP to driver
        if (rideDto.getDriver() != null) {
            rideDto.getDriver().setRcUrl(null);
            rideDto.getDriver().setLicenseUrl(null);
            rideDto.getDriver().setInsuranceUrl(null);
            rideDto.getDriver().setRejectionReason(null);
        }
        return rideDto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        Driver currentDriver = getCurrentDriver();
        return rideService.getAllRidesOfDriver(currentDriver, pageRequest).map(ride -> {
            RideDto rideDto = toDriverRideDto(ride);
            rideDto.setRiderRating(ratingService.getRiderRating(ride));
            return rideDto;
        });
    }

    @Override
    @Transactional
    public DriverDto updateLocation(PointDto pointDto) {
        Driver driver = getCurrentDriver();
        Point point = GeometryUtil.createPoint(pointDto);
        driver.setCurrentLocation(point);
        return modelMapper.map(driverRepository.save(driver), DriverDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public RideRequestDto getIncomingRideRequest() {
        Driver currentDriver = getCurrentDriver();
        List<RideRequest> requests = rideRequestRepository
                .findByNotifiedDriversContainingAndRideRequestStatus(currentDriver, RideRequestStatus.PENDING);
        if (requests == null || requests.isEmpty()) return null;
        RideRequestDto dto = modelMapper.map(requests.get(0), RideRequestDto.class);
        dto.setOtp(null); // Never leak OTP to driver
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Driver getCurrentDriver() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver not associated with user id=" + user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public DriverDto updateDriverAvailability(boolean available) {
        Driver driver = getCurrentDriver();
        return modelMapper.map(updateDriverAvailability(driver, available), DriverDto.class);
    }

    @Override
    public Driver updateDriverAvailability(Driver driver, boolean available) {
        if (available && Boolean.FALSE.equals(driver.getVehicleVerified())) {
            throw new RuntimeException("Vehicle verification pending. You cannot go online.");
        }
        if (available && Boolean.TRUE.equals(driver.getBlocked())) {
            throw new RuntimeException("Your account is blocked by admin. Please contact support.");
        }
        driver.setAvailable(available);
        return driverRepository.save(driver);
    }

    @Override
    @Transactional
    public void blockDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        driver.setBlocked(true);
        driver.setAvailable(false); // Force offline
        driverRepository.save(driver);
        log.info("Driver id={} has been BLOCKED by admin", driverId);
    }

    @Override
    @Transactional
    public void unblockDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        driver.setBlocked(false);
        driverRepository.save(driver);
        log.info("Driver id={} has been UNBLOCKED by admin", driverId);
    }

    @Override
    public Driver createNewDriver(Driver driver) {
        driver.setVerificationStatus(DriverVerificationStatus.PENDING);
        driver.setVehicleVerified(false);
        driver.setVerificationSubmitted(false);
        driver.setAvailable(false);
        return driverRepository.save(driver);
    }

    // --- Verification Methods Implementation ---

    @Override
    @Transactional
    public String uploadDocument(org.springframework.web.multipart.MultipartFile file, String docType) {
        Driver driver = getCurrentDriver();
        String url = documentStorageService.uploadFile(file, "drivers/" + driver.getId());

        switch (docType.toLowerCase()) {
            case "rc" -> driver.setRcUrl(url);
            case "license" -> driver.setLicenseUrl(url);
            case "insurance" -> driver.setInsuranceUrl(url);
            case "profile", "profile-photo" -> driver.setProfilePhotoUrl(url);
            default -> throw new RuntimeException("Invalid document type: " + docType);
        }
        
        driverRepository.save(driver);
        return url;
    }

    @Override
    @Transactional
    public void submitVerification() {
        Driver driver = getCurrentDriver();
        if (driver.getRcUrl() == null || driver.getLicenseUrl() == null || driver.getInsuranceUrl() == null) {
            throw new RuntimeException("Please upload all required documents before submitting.");
        }
        if (Boolean.TRUE.equals(driver.getVerificationSubmitted())
                && DriverVerificationStatus.PENDING.equals(driver.getVerificationStatus())) {
            throw new RuntimeException("Your documents are already submitted and awaiting admin review.");
        }
        driver.setVerificationStatus(DriverVerificationStatus.PENDING);
        driver.setVerificationSubmitted(true);
        driver.setRejectionReason(null);
        driverRepository.save(driver);
        log.info("Driver id={} submitted documents for verification", driver.getId());
        notifyAdminOfVerificationSubmission(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverVerificationDto getDriverVerificationDetails(Long driverId) {
        Driver driver = driverRepository.findByIdWithUser(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        return driverVerificationMapper.toDto(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DriverVerificationDto> getPendingDrivers(PageRequest pageRequest) {
        return driverRepository.findSubmittedByStatusWithUser(
                DriverVerificationStatus.PENDING,
                pageRequest
        ).map(driverVerificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DriverVerificationDto> getAllDriversByStatus(DriverVerificationStatus status, PageRequest pageRequest) {
        Page<Driver> page = DriverVerificationStatus.PENDING.equals(status)
                ? driverRepository.findSubmittedByStatusWithUser(status, pageRequest)
                : driverRepository.findByStatusWithUser(status, pageRequest);
        return page.map(driverVerificationMapper::toDto);
    }

    @Override
    @Transactional
    public void approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        
        driver.setVehicleVerified(true);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setRejectionReason(null);
        driver.setVerificationSubmitted(true);
        driverRepository.save(driver);
        
        log.info("Driver id={} approved by admin", driverId);
        notifyDriverOfVerificationDecision(driver, true, null);
    }

    @Override
    @Transactional
    public void rejectDriver(Long driverId, String reason) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));
        
        driver.setVehicleVerified(false);
        driver.setVerificationStatus(DriverVerificationStatus.REJECTED);
        driver.setRejectionReason(reason);
        driver.setVerificationSubmitted(false);
        driver.setAvailable(false);
        driverRepository.save(driver);
        
        log.info("Driver id={} rejected by admin. Reason: {}", driverId, reason);
        notifyDriverOfVerificationDecision(driver, false, reason);
    }

    @Override
    @Transactional
    public void autoApproveDriverForDev(Long driverId) {
        Driver target = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + driverId));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            Driver current = getCurrentDriver();
            if (!current.getId().equals(driverId)) {
                throw new RuntimeException("Drivers can only auto-approve their own account in dev mode.");
            }
        }

        target.setVehicleVerified(true);
        target.setVerificationStatus(DriverVerificationStatus.APPROVED);
        target.setVerificationSubmitted(true);
        target.setRejectionReason(null);
        driverRepository.save(target);
        log.info("Driver id={} auto-approved via dev endpoint", driverId);
        notifyDriverOfVerificationDecision(target, true, null);
    }

    private void notifyAdminOfVerificationSubmission(Driver driver) {
        String adminEmail = appAdminProperties.getNotificationEmail();
        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("Admin notification email not configured; skipping verification alert for driver id={}", driver.getId());
            return;
        }
        try {
            String driverName = driver.getUser() != null ? driver.getUser().getName() : "Driver #" + driver.getId();
            String body = String.format(
                    """
                    A driver has submitted vehicle verification documents for review.

                    Driver ID   : %d
                    Name        : %s
                    Vehicle ID  : %s
                    Status      : PENDING

                    Please review the documents in the admin verification dashboard.
                    """,
                    driver.getId(),
                    driverName,
                    driver.getVehicleId() != null ? driver.getVehicleId() : "N/A"
            );
            emailSenderService.sendEmail(
                    adminEmail,
                    "BookCar – New driver verification submission",
                    body
            );
        } catch (Exception e) {
            log.warn("Could not notify admin about driver verification submission id={}: {}", driver.getId(), e.getMessage());
        }
    }

    private void notifyDriverOfVerificationDecision(Driver driver, boolean approved, String rejectionReason) {
        if (driver.getUser() == null || driver.getUser().getEmail() == null || driver.getUser().getEmail().isBlank()) {
            log.info("Driver id={} has no email; skipping verification decision notification", driver.getId());
            return;
        }
        try {
            String driverName = driver.getUser().getName() != null ? driver.getUser().getName() : "Driver";
            String subject;
            String body;
            if (approved) {
                subject = "BookCar – You are verified! Start accepting rides";
                body = String.format(
                        """
                        Hi %s,

                        Great news — your vehicle verification has been approved.

                        You can now go online from the driver panel and start accepting rides.

                        Thank you for driving with BookCar!
                        """,
                        driverName
                );
            } else {
                subject = "BookCar – Verification needs attention";
                String reason = rejectionReason != null && !rejectionReason.isBlank()
                        ? rejectionReason
                        : "One or more documents were invalid or unclear.";
                body = String.format(
                        """
                        Hi %s,

                        Your vehicle verification was not approved.

                        Reason: %s

                        Please re-upload the correct documents from the verification page and submit again.

                        BookCar Team
                        """,
                        driverName,
                        reason
                );
            }
            emailSenderService.sendEmail(driver.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Could not notify driver id={} about verification decision: {}", driver.getId(), e.getMessage());
        }
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

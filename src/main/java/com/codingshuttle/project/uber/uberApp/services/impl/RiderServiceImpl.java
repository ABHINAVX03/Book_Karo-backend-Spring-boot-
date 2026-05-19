package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.*;
import com.codingshuttle.project.uber.uberApp.entities.*;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideRequestStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.RideStatus;
import com.codingshuttle.project.uber.uberApp.exceptions.InvalidRideStatusException;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.exceptions.UnauthorizedAccessException;
import com.codingshuttle.project.uber.uberApp.repositories.RideRequestRepository;
import com.codingshuttle.project.uber.uberApp.repositories.RiderRepository;
import com.codingshuttle.project.uber.uberApp.services.*;
import com.codingshuttle.project.uber.uberApp.strategies.RideStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiderServiceImpl implements RiderService {

    private final ModelMapper modelMapper;
    private final RideStrategyManager rideStrategyManager;
    private final RideRequestRepository rideRequestRepository;
    private final RiderRepository riderRepository;
    private final RideService rideService;
    private final DriverService driverService;
    private final RatingService ratingService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final EmailSenderService emailSenderService;
    private final OtpService otpService;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    private final RestClient razorpayRestClient = RestClient.builder()
            .baseUrl("https://api.razorpay.com/v1")
            .requestFactory(razorpayRequestFactory())
            .build();

    private static final DateTimeFormatter RECEIPT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ─── Estimate Fare ────────────────────────────────────────────────────────
    @Override
    public RideRequestDto estimateFare(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();
        RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);
        BigDecimal fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);
        return modelMapper.map(rideRequest, RideRequestDto.class);
    }

    // ─── Request Ride ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public RideRequestDto requestRide(RideRequestDto rideRequestDto) {
        Rider rider = getCurrentRider();

        // Validate coordinates — reject obvious zero/null island submissions
        if (rideRequestDto.getPickupLocation() == null || rideRequestDto.getDropOffLocation() == null) {
            throw new IllegalArgumentException("Pickup and dropoff locations are required");
        }

        RideRequest rideRequest = modelMapper.map(rideRequestDto, RideRequest.class);
        rideRequest.setRideRequestStatus(RideRequestStatus.PENDING);
        rideRequest.setRider(rider);
        BigDecimal fare = rideStrategyManager.rideFareCalculationStrategy().calculateFare(rideRequest);
        rideRequest.setFare(fare);
        String otp = otpService.generateRideOtp();
        rideRequest.setOtp(otp);

        RideRequest savedRideRequest = rideRequestRepository.save(rideRequest);
        log.info("RideRequest id={} created for rider id={}, fare={}", savedRideRequest.getId(), rider.getId(), fare);

        List<Driver> matchedDrivers = rideStrategyManager
                .driverMatchingStrategy(rider.getRating())
                .findMatchingDriver(savedRideRequest);

        if (matchedDrivers.isEmpty()) {
            log.warn("No available drivers found for RideRequest id={} — riders pickup: {}",
                    savedRideRequest.getId(), savedRideRequest.getPickupLocation());
        }

        savedRideRequest.setNotifiedDrivers(matchedDrivers);
        rideRequestRepository.save(savedRideRequest);
        notificationService.notifyDriversAboutRideRequest(savedRideRequest, matchedDrivers);

        return modelMapper.map(savedRideRequest, RideRequestDto.class);
    }

    // ─── Cancel Ride Request ──────────────────────────────────────────────────
    @Override
    @Transactional
    public RideRequestDto cancelRideRequest(Long rideRequestId) {
        Rider rider = getCurrentRider();
        RideRequest rideRequest = rideRequestRepository.findById(rideRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest not found with id: " + rideRequestId));
        if (!rider.equals(rideRequest.getRider())) {
            throw new UnauthorizedAccessException("Rider id=" + rider.getId() + " does not own ride request id=" + rideRequestId);
        }
        if (!rideRequest.getRideRequestStatus().equals(RideRequestStatus.PENDING)) {
            throw new InvalidRideStatusException("Ride request id=" + rideRequestId + " cannot be cancelled; current status: " + rideRequest.getRideRequestStatus());
        }
        rideRequest.setRideRequestStatus(RideRequestStatus.CANCELLED);
        RideRequest savedRideRequest = rideRequestRepository.save(rideRequest);
        log.info("RideRequest id={} cancelled by rider id={}", rideRequestId, rider.getId());
        return modelMapper.map(savedRideRequest, RideRequestDto.class);
    }

    // ─── Cancel Ride ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public RideDto cancelRide(Long rideId) {
        Rider rider = getCurrentRider();
        Ride ride = rideService.getRideById(rideId);
        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException("Rider id=" + rider.getId() + " does not own ride id=" + rideId);
        }
        if (!ride.getRideStatus().equals(RideStatus.CONFIRMED)) {
            throw new InvalidRideStatusException("Ride id=" + rideId + " cannot be cancelled; current status: " + ride.getRideStatus());
        }
        Ride savedRide = rideService.updateRideStatus(ride, RideStatus.CANCELLED);
        driverService.updateDriverAvailability(ride.getDriver(), true);
        notificationService.notifyDriverAboutRideCancellation(ride.getDriver(), rideId);
        log.info("Ride id={} cancelled by rider id={}", rideId, rider.getId());
        return modelMapper.map(savedRide, RideDto.class);
    }

    // ─── Rate Driver ──────────────────────────────────────────────────────────
    @Override
    public DriverDto rateDriver(Long rideId, Integer rating) {
        Ride ride = rideService.getRideById(rideId);
        Rider rider = getCurrentRider();
        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException("Rider id=" + rider.getId() + " is not the owner of ride id=" + rideId);
        }
        if (!ride.getRideStatus().equals(RideStatus.ENDED)) {
            throw new InvalidRideStatusException("Cannot rate driver for ride id=" + rideId + "; ride must be ENDED but is: " + ride.getRideStatus());
        }
        log.info("Rider id={} rating driver for ride id={} with rating={}", rider.getId(), rideId, rating);
        return ratingService.rateDriver(ride, rating);
    }

    // ─── Profile & Rides ─────────────────────────────────────────────────────
    @Override
    public RiderDto getMyProfile() {
        return modelMapper.map(getCurrentRider(), RiderDto.class);
    }

    @Override
    public Page<RideDto> getAllMyRides(PageRequest pageRequest) {
        return rideService.getAllRidesOfRider(getCurrentRider(), pageRequest)
                .map(ride -> modelMapper.map(ride, RideDto.class));
    }

    /**
     * NEW — FIX BUG-07.
     *
     * Returns the rider's most recent CONFIRMED or ONGOING ride directly,
     * without scanning through all ride history. This is called by the new
     * GET /riders/currentRide endpoint and used by BookRidePage.jsx to
     * reliably detect when a driver has accepted their ride request.
     *
     * The old approach searched through the first 10 paginated rides using
     * coordinate matching — it could miss the ride entirely if the rider
     * had many past rides, or falsely match an old ride with the same route.
     */
    @Override
    public RideDto getCurrentActiveRide() {
        Rider rider = getCurrentRider();

        // 1. Try to find CONFIRMED or ONGOING ride
        Optional<Ride> activeRide = rideService.getCurrentActiveRideForRider(rider);
        if (activeRide.isPresent()) {
            return modelMapper.map(activeRide.get(), RideDto.class);
        }

        // 2. If no CONFIRMED/ONGOING, check for the most recent ENDED ride that hasn't been rated
        // We use page 0, size 1, sorted by ID desc to get the very last ride
        Page<Ride> lastRides = rideService.getAllRidesOfRider(rider, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")));
        if (!lastRides.isEmpty()) {
            Ride lastRide = lastRides.getContent().get(0);
            // If the last ride is ENDED and rider hasn't rated the driver yet, it's still "active" for the UI
            if (RideStatus.ENDED.equals(lastRide.getRideStatus())) {
                if (ratingService.getDriverRating(lastRide) == null) {
                    return modelMapper.map(lastRide, RideDto.class);
                }
            }
        }

        return null;
    }

    @Override
    public Rider createNewRider(User user) {
        Rider rider = Rider.builder().user(user).rating(0.0).build();
        Rider saved = riderRepository.save(rider);
        log.info("New rider id={} created for user id={}", saved.getId(), user.getId());
        return saved;
    }

    @Override
    public Rider getCurrentRider() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return riderRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not associated with user id=" + user.getId()));
    }

    // ─── Razorpay Ride Payment ────────────────────────────────────────────────

    @Override
    public RidePaymentOrderDto createRidePaymentOrder(Long rideId) {
        Rider rider = getCurrentRider();
        Ride ride = rideService.getRideById(rideId);

        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException("Rider does not own ride id=" + rideId);
        }
        if (!ride.getRideStatus().equals(RideStatus.ENDED)) {
            throw new InvalidRideStatusException("Ride id=" + rideId + " must be ENDED to create payment order");
        }
        if (!PaymentMethod.RAZORPAY.equals(ride.getPaymentMethod())) {
            throw new RuntimeException("Ride id=" + rideId + " is not a RAZORPAY ride");
        }

        Payment payment = paymentService.getPaymentForRideWithLock(ride);

        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            return RidePaymentOrderDto.builder()
                    .key(razorpayKeyId)
                    .orderId(payment.getProviderOrderId())
                    .amount(ride.getFare().multiply(new BigDecimal("100")).intValue())
                    .currency(payment.getCurrency() == null ? "INR" : payment.getCurrency())
                    .name("BookCar")
                    .rideId(rideId)
                    .description("Payment already completed for ride #" + rideId)
                    .build();
        }

        int amountInPaise = ride.getFare().multiply(new BigDecimal("100")).intValue();
        if (payment.getProviderOrderId() != null && !payment.getProviderOrderId().isBlank()) {
            return RidePaymentOrderDto.builder()
                    .key(razorpayKeyId)
                    .orderId(payment.getProviderOrderId())
                    .amount(amountInPaise)
                    .currency(payment.getCurrency() == null ? "INR" : payment.getCurrency())
                    .name("BookCar")
                    .rideId(rideId)
                    .description("Payment for ride #" + rideId)
                    .build();
        }

        Map orderResponse;
        try {
            orderResponse = razorpayRestClient.post()
                    .uri("/orders")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountInPaise, "currency", "INR", "receipt", "ride_" + rideId))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        log.error("Razorpay order creation failed: {} {}", response.getStatusCode(), response.getStatusText());
                        throw new RuntimeException("Razorpay error: " + response.getStatusCode());
                    })
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Could not create Razorpay order for ride id={}: {}", rideId, e.getMessage());
            throw new RuntimeException("Could not initiate Razorpay payment: " + e.getMessage());
        }

        String razorpayOrderId = (String) orderResponse.get("id");
        paymentService.recordGatewayDetails(payment, razorpayOrderId, null, null, "INR", null);
        log.info("Razorpay order {} created for ride id={}, amount={}paise", razorpayOrderId, rideId, amountInPaise);

        return RidePaymentOrderDto.builder()
                .key(razorpayKeyId)
                .orderId(razorpayOrderId)
                .amount(amountInPaise)
                .currency("INR")
                .name("BookCar")
                .rideId(rideId)
                .description("Payment for ride #" + rideId)
                .build();
    }

    @Override
    @Transactional
    public RideDto verifyAndCompleteRidePayment(Long rideId, WalletPaymentVerificationDto dto) {
        Rider rider = getCurrentRider();
        Ride ride = rideService.getRideById(rideId);

        if (!rider.equals(ride.getRider())) {
            throw new UnauthorizedAccessException("Rider does not own ride id=" + rideId);
        }
        if (!ride.getRideStatus().equals(RideStatus.ENDED)) {
            throw new InvalidRideStatusException("Ride id=" + rideId + " must be ENDED");
        }

        Payment payment = paymentService.getPaymentForRideWithLock(ride);
        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            if (dto.getRazorpayPaymentId().equals(payment.getProviderPaymentId())) {
                return modelMapper.map(ride, RideDto.class);
            }
            throw new IllegalStateException("Payment for this ride has already been settled with a different provider reference.");
        }

        if (payment.getProviderOrderId() != null && !payment.getProviderOrderId().equals(dto.getRazorpayOrderId())) {
            throw new IllegalStateException("Razorpay order does not belong to this ride.");
        }

        verifyRazorpaySignature(dto.getRazorpayOrderId(), dto.getRazorpayPaymentId(), dto.getRazorpaySignature());
        Map<String, Object> paymentDetails = fetchRazorpayPayment(dto.getRazorpayPaymentId());
        verifyGatewayPayment(payment, dto, paymentDetails);
        paymentService.recordGatewayDetails(
                payment,
                dto.getRazorpayOrderId(),
                dto.getRazorpayPaymentId(),
                dto.getRazorpaySignature(),
                String.valueOf(paymentDetails.getOrDefault("currency", "INR")),
                dto.getRazorpayPaymentId()
        );
        paymentService.processPayment(ride);
        sendRideReceiptEmail(ride, rider.getUser());

        log.info("Ride id={} payment verified and completed for rider id={}", rideId, rider.getId());
        return modelMapper.map(ride, RideDto.class);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void verifyRazorpaySignature(String orderId, String paymentId, String receivedSignature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hash) {
                hexBuilder.append(String.format("%02x", b));
            }
            if (!hexBuilder.toString().equals(receivedSignature)) {
                throw new RuntimeException("Payment verification failed: invalid signature");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Payment signature verification error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> fetchRazorpayPayment(String paymentId) {
        return razorpayRestClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {
                    throw new IllegalStateException("Could not verify Razorpay payment state: " + response.getStatusCode());
                })
                .body(Map.class);
    }

    private void verifyGatewayPayment(Payment payment, WalletPaymentVerificationDto dto, Map<String, Object> paymentDetails) {
        String status = String.valueOf(paymentDetails.get("status"));
        String currency = String.valueOf(paymentDetails.get("currency"));
        String orderId = String.valueOf(paymentDetails.get("order_id"));
        Number amount = (Number) paymentDetails.get("amount");

        if (!"captured".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Razorpay payment is not captured.");
        }
        if (!dto.getRazorpayOrderId().equals(orderId)) {
            throw new IllegalStateException("Razorpay payment order mismatch.");
        }
        if (!"INR".equalsIgnoreCase(currency)) {
            throw new IllegalStateException("Razorpay payment currency mismatch.");
        }

        int expectedAmount = payment.getAmount().multiply(new BigDecimal("100")).intValue();
        if (amount == null || amount.intValue() != expectedAmount) {
            throw new IllegalStateException("Razorpay payment amount mismatch.");
        }
    }

    private String basicAuthHeader() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
    }

    private SimpleClientHttpRequestFactory razorpayRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }

    void sendRideReceiptEmail(Ride ride, User riderUser) {
        try {
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
                ride.getId(), endedAt, driverName, ride.getPaymentMethod(), ride.getFare()
            );

            emailSenderService.sendEmail(riderUser.getEmail(), "BookCar Ride Receipt – #" + ride.getId(), body);
        } catch (Exception e) {
            log.warn("Could not send ride receipt email for ride id={}: {}", ride.getId(), e.getMessage());
        }
    }
}

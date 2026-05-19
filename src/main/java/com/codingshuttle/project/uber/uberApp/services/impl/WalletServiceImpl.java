package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.RideDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletPaymentOrderDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletPaymentVerificationDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletTransactionDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.Wallet;
import com.codingshuttle.project.uber.uberApp.entities.WalletTransaction;
import com.codingshuttle.project.uber.uberApp.entities.enums.TransactionMethod;
import com.codingshuttle.project.uber.uberApp.entities.enums.TransactionType;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.exceptions.RuntimeConflictException;
import com.codingshuttle.project.uber.uberApp.repositories.WalletRepository;
import com.codingshuttle.project.uber.uberApp.repositories.WalletTransactionRepository;
import com.codingshuttle.project.uber.uberApp.services.WalletService;
import com.codingshuttle.project.uber.uberApp.services.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionService walletTransactionService;
    private final ModelMapper modelMapper;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    private final RestClient razorpayClient = RestClient.builder()
            .baseUrl("https://api.razorpay.com/v1")
            .build();

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#user.getId()")
    public Wallet addMoneyToWallet(User user, BigDecimal amount, String transactionId, Ride ride, TransactionMethod transactionMethod) {
        validateAmount(amount);
        Wallet wallet = findByUserForUpdate(user);
        wallet.setBalance(wallet.getBalance().add(amount));

        WalletTransaction walletTransaction = WalletTransaction.builder()
                .transactionId(transactionId)
                .ride(ride)
                .wallet(wallet)
                .transactionType(TransactionType.CREDIT)
                .transactionMethod(transactionMethod)
                .amount(amount)
                .build();

        walletTransactionService.createNewWalletTransaction(walletTransaction);

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#user.getId()")
    public Wallet deductMoneyFromWallet(User user, BigDecimal amount,
                                        String transactionId, Ride ride,
                                        TransactionMethod transactionMethod) {
        validateAmount(amount);
        Wallet wallet = findByUserForUpdate(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeConflictException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));

        WalletTransaction walletTransaction = WalletTransaction.builder()
                .transactionId(transactionId)
                .ride(ride)
                .wallet(wallet)
                .transactionType(TransactionType.DEBIT)
                .transactionMethod(transactionMethod)
                .amount(amount)
                .build();

        walletTransactionService.createNewWalletTransaction(walletTransaction);

//        wallet.getTransactions().add(walletTransaction);

        return walletRepository.save(wallet);
    }

    @Override
    public void withdrawAllMyMoneyFromWallet() {

    }

    @Override
    public Wallet findWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: "+walletId));
    }

    @Override
    public Wallet createNewWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet findByUser(User user) {
        return walletRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user with id: "+user.getId()));
    }

    private Wallet findByUserForUpdate(User user) {
        return walletRepository.findByUserForUpdate(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user with id: " + user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "wallets", key = "#root.target.getCurrentUserIdForCache()")
    public WalletDto getMyWallet() {
        return toWalletDto(findByUser(getCurrentUser()));
    }

    public Long getCurrentUserIdForCache() {
        return getCurrentUser().getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#root.target.getCurrentUserIdForCache()")
    public WalletDto addMoneyToMyWallet(BigDecimal amount) {
        Wallet wallet = addMoneyToWallet(
                getCurrentUser(),
                amount,
                UUID.randomUUID().toString(),
                null,
                TransactionMethod.BANKING
        );
        return toWalletDto(wallet);
    }

    @Override
    public WalletPaymentOrderDto createWalletTopUpOrder(BigDecimal amount) {
        validateAmount(amount);
        ensureRazorpayConfigured();

        int amountInPaise = toPaise(amount);
        JsonNode order = razorpayClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", amountInPaise,
                        "currency", "INR",
                        "receipt", "wallet_" + getCurrentUser().getId() + "_" + System.currentTimeMillis()
                ))
                .retrieve()
                .body(JsonNode.class);

        if (order == null || order.get("id") == null) {
            throw new RuntimeConflictException("Unable to create payment order");
        }

        return new WalletPaymentOrderDto(
                razorpayKeyId,
                order.get("id").asText(),
                order.get("amount").asInt(),
                order.get("currency").asText("INR"),
                "BookCar Wallet"
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#root.target.getCurrentUserIdForCache()")
    public WalletDto verifyWalletTopUpPayment(WalletPaymentVerificationDto verificationDto) {
        ensureRazorpayConfigured();
        validateVerificationPayload(verificationDto);

        String generatedSignature = hmacSha256(
                verificationDto.getRazorpayOrderId() + "|" + verificationDto.getRazorpayPaymentId(),
                razorpayKeySecret
        );

        if (!MessageDigest.isEqual(
                generatedSignature.getBytes(StandardCharsets.UTF_8),
                verificationDto.getRazorpaySignature().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new RuntimeConflictException("Payment verification failed");
        }

        if (walletTransactionRepository.findByTransactionId(verificationDto.getRazorpayPaymentId()).isPresent()) {
            return getMyWallet();
        }

        JsonNode payment = razorpayClient
                .get()
                .uri("/payments/{paymentId}", verificationDto.getRazorpayPaymentId())
                .headers(headers -> headers.setBasicAuth(razorpayKeyId, razorpayKeySecret))
                .retrieve()
                .body(JsonNode.class);

        validateCapturedPayment(payment, verificationDto.getRazorpayOrderId());

        BigDecimal amount = new BigDecimal(payment.get("amount").asInt()).divide(new BigDecimal("100.0"));
        TransactionMethod method = toTransactionMethod(payment.path("method").asText());
        Wallet wallet = addMoneyToWallet(
                getCurrentUser(),
                amount,
                verificationDto.getRazorpayPaymentId(),
                null,
                method
        );

        return toWalletDto(wallet);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#root.target.getCurrentUserIdForCache()")
    public WalletDto withdrawMoneyFromMyWallet(BigDecimal amount) {
        Wallet wallet = deductMoneyFromWallet(
                getCurrentUser(),
                amount,
                UUID.randomUUID().toString(),
                null,
                TransactionMethod.BANKING
        );
        return toWalletDto(wallet);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeConflictException("Amount must be greater than 0");
        }
        if (amount.scale() > 2) {
            throw new RuntimeConflictException("Amount can have at most 2 decimal places");
        }
        if (amount.compareTo(new BigDecimal("100000.00")) > 0) {
            throw new RuntimeConflictException("Amount exceeds the allowed limit");
        }
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() ||
                razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new RuntimeConflictException("Payment gateway is not configured");
        }
    }

    private int toPaise(BigDecimal amount) {
        long paise = amount.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        if (paise <= 0 || paise > Integer.MAX_VALUE) {
            throw new RuntimeConflictException("Invalid payment amount");
        }
        return (int) paise;
    }

    private void validateVerificationPayload(WalletPaymentVerificationDto verificationDto) {
        if (verificationDto == null ||
                verificationDto.getRazorpayOrderId() == null ||
                verificationDto.getRazorpayPaymentId() == null ||
                verificationDto.getRazorpaySignature() == null) {
            throw new RuntimeConflictException("Missing payment verification details");
        }
    }

    private void validateCapturedPayment(JsonNode payment, String orderId) {
        if (payment == null ||
                !orderId.equals(payment.path("order_id").asText()) ||
                !"captured".equalsIgnoreCase(payment.path("status").asText()) ||
                payment.path("amount").asInt(0) <= 0) {
            throw new RuntimeConflictException("Payment has not been captured");
        }
    }

    private TransactionMethod toTransactionMethod(String method) {
        return switch (String.valueOf(method).toLowerCase()) {
            case "upi" -> TransactionMethod.UPI;
            case "card" -> TransactionMethod.CARD;
            case "netbanking" -> TransactionMethod.NETBANKING;
            case "wallet" -> TransactionMethod.WALLET;
            default -> TransactionMethod.BANKING;
        };
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new RuntimeConflictException("Payment verification failed");
        }
    }

    private WalletDto toWalletDto(Wallet wallet) {
        WalletDto walletDto = new WalletDto();
        walletDto.setId(wallet.getId());
        walletDto.setUser(modelMapper.map(wallet.getUser(), com.codingshuttle.project.uber.uberApp.dto.UserDto.class));
        walletDto.setBalance(wallet.getBalance());

        List<WalletTransactionDto> transactions = walletTransactionRepository
                .findByWalletOrderByTimeStampDesc(wallet)
                .stream()
                .map(this::toWalletTransactionDto)
                .toList();

        walletDto.setTransactions(transactions);
        return walletDto;
    }

    private WalletTransactionDto toWalletTransactionDto(WalletTransaction walletTransaction) {
        WalletTransactionDto walletTransactionDto = WalletTransactionDto.builder()
                .id(walletTransaction.getId())
                .amount(walletTransaction.getAmount())
                .transactionId(walletTransaction.getTransactionId())
                .transactionMethod(walletTransaction.getTransactionMethod())
                .transactionType(walletTransaction.getTransactionType())
                .timeStamp(walletTransaction.getTimeStamp())
                .build();

        if (walletTransaction.getRide() != null) {
            walletTransactionDto.setRide(modelMapper.map(walletTransaction.getRide(), RideDto.class));
        }

        return walletTransactionDto;
    }
}

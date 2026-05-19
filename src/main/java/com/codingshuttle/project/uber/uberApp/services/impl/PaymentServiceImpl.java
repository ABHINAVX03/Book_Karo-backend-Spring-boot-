package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.entities.Payment;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentStatus;
import com.codingshuttle.project.uber.uberApp.exceptions.ResourceNotFoundException;
import com.codingshuttle.project.uber.uberApp.repositories.PaymentRepository;
import com.codingshuttle.project.uber.uberApp.services.PaymentService;
import com.codingshuttle.project.uber.uberApp.strategies.PaymentStrategyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyManager paymentStrategyManager;

    @Override
    @Transactional
    public void processPayment(Ride ride) {
        Payment payment = getPaymentForRideWithLock(ride);
        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            return;
        }
        paymentStrategyManager.paymentStrategy(payment.getPaymentMethod()).processPayment(payment);
    }

    @Override
    @Transactional
    public Payment createNewPayment(Ride ride) {
        return paymentRepository.findByRide(ride).orElseGet(() -> paymentRepository.save(Payment.builder()
                .ride(ride)
                .paymentMethod(ride.getPaymentMethod())
                .amount(ride.getFare())
                .currency("INR")
                .paymentStatus(PaymentStatus.PENDING)
                .build()));
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Payment payment, PaymentStatus status) {
        payment.setPaymentStatus(status);
        payment.setProcessedAt(PaymentStatus.CONFIRMED.equals(status) ? LocalDateTime.now() : payment.getProcessedAt());
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentForRideWithLock(Ride ride) {
        return paymentRepository.findByRideForUpdate(ride)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride with id: " + ride.getId()));
    }

    @Override
    @Transactional
    public Payment recordGatewayDetails(Payment payment, String orderId, String paymentId, String signature, String currency, String settlementReference) {
        payment.setProviderOrderId(orderId);
        payment.setProviderPaymentId(paymentId);
        payment.setProviderSignature(signature);
        payment.setCurrency(currency);
        payment.setSettlementReference(settlementReference);
        payment.setProcessedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
}

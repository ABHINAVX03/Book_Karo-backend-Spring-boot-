package com.codingshuttle.project.uber.uberApp.strategies.impl;

import com.codingshuttle.project.uber.uberApp.entities.Payment;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentStatus;
import com.codingshuttle.project.uber.uberApp.repositories.PaymentRepository;
import com.codingshuttle.project.uber.uberApp.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashPaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;

    @Override
    public void processPayment(Payment payment) {
        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            return;
        }
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        payment.setSettlementReference("ride-cash-" + payment.getRide().getId());
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}

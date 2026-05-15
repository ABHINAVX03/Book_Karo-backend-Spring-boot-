package com.codingshuttle.project.uber.uberApp.strategies;

import com.codingshuttle.project.uber.uberApp.entities.Payment;

import java.math.BigDecimal;

public interface PaymentStrategy {
    BigDecimal PLATFORM_COMMISSION = new BigDecimal("0.3");
    void processPayment(Payment payment);

}

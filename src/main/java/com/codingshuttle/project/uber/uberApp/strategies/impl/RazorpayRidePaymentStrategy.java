package com.codingshuttle.project.uber.uberApp.strategies.impl;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Payment;
import com.codingshuttle.project.uber.uberApp.entities.enums.PaymentStatus;
import com.codingshuttle.project.uber.uberApp.entities.enums.TransactionMethod;
import com.codingshuttle.project.uber.uberApp.repositories.PaymentRepository;
import com.codingshuttle.project.uber.uberApp.services.WalletService;
import com.codingshuttle.project.uber.uberApp.strategies.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Strategy for rides paid via Razorpay (card/UPI/netbanking).
 * The rider's payment is collected by Razorpay externally.
 * This strategy only: credits the driver wallet (70%) and marks payment CONFIRMED.
 */
@Service
@RequiredArgsConstructor
public class RazorpayRidePaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void processPayment(Payment payment) {
        Driver driver = payment.getRide().getDriver();

        BigDecimal driversCut = payment.getAmount().multiply(BigDecimal.ONE.subtract(PLATFORM_COMMISSION));

        walletService.addMoneyToWallet(
                driver.getUser(),
                driversCut,
                null,
                payment.getRide(),
                TransactionMethod.CARD
        );

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);
    }
}
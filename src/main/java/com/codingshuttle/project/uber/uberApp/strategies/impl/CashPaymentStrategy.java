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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashPaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public void processPayment(Payment payment) {
        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            return;
        }

        Driver driver = payment.getRide().getDriver();
        String settlementReference = "ride-cash-" + payment.getRide().getId();

        // Driver received 100% cash from rider. Deduct platform commission (30%) from driver's wallet.
        BigDecimal platformCommission = payment.getAmount().multiply(PLATFORM_COMMISSION);
        walletService.deductMoneyFromWalletAllowingNegative(
                driver.getUser(),
                platformCommission,
                settlementReference + "-commission",
                payment.getRide(),
                TransactionMethod.RIDE
        );

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        payment.setSettlementReference(settlementReference);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}

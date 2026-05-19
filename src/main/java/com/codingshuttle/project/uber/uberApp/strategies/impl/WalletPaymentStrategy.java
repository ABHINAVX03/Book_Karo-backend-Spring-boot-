package com.codingshuttle.project.uber.uberApp.strategies.impl;

import com.codingshuttle.project.uber.uberApp.entities.Driver;
import com.codingshuttle.project.uber.uberApp.entities.Payment;
import com.codingshuttle.project.uber.uberApp.entities.Rider;
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
public class WalletPaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void processPayment(Payment payment) {
        if (PaymentStatus.CONFIRMED.equals(payment.getPaymentStatus())) {
            return;
        }
        Driver driver = payment.getRide().getDriver();
        Rider rider = payment.getRide().getRider();
        String settlementReference = "ride-wallet-" + payment.getRide().getId();

        walletService.deductMoneyFromWallet(rider.getUser(),
                payment.getAmount(), settlementReference + "-debit", payment.getRide(), TransactionMethod.RIDE);

        BigDecimal driversCut = payment.getAmount().multiply(BigDecimal.ONE.subtract(PLATFORM_COMMISSION));

        walletService.addMoneyToWallet(driver.getUser(),
                driversCut, settlementReference + "-credit", payment.getRide(), TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        payment.setSettlementReference(settlementReference);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}

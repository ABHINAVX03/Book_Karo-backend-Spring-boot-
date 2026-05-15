package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.dto.WalletDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletPaymentOrderDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletPaymentVerificationDto;
import com.codingshuttle.project.uber.uberApp.entities.Ride;
import com.codingshuttle.project.uber.uberApp.entities.User;
import com.codingshuttle.project.uber.uberApp.entities.Wallet;
import com.codingshuttle.project.uber.uberApp.entities.enums.TransactionMethod;

import java.math.BigDecimal;

public interface WalletService {

    Wallet addMoneyToWallet(User user, BigDecimal amount,
                            String transactionId, Ride ride,
                            TransactionMethod transactionMethod);

    Wallet deductMoneyFromWallet(User user, BigDecimal amount,
                                 String transactionId, Ride ride,
                                 TransactionMethod transactionMethod);

    void withdrawAllMyMoneyFromWallet();

    Wallet findWalletById(Long walletId);

    Wallet createNewWallet(User user);

    Wallet findByUser(User user);

    WalletDto getMyWallet();

    WalletDto addMoneyToMyWallet(BigDecimal amount);

    WalletPaymentOrderDto createWalletTopUpOrder(BigDecimal amount);

    WalletDto verifyWalletTopUpPayment(WalletPaymentVerificationDto verificationDto);

    WalletDto withdrawMoneyFromMyWallet(BigDecimal amount);

}

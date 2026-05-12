package com.codingshuttle.project.uber.uberApp.services.impl;

import com.codingshuttle.project.uber.uberApp.dto.RideDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletDto;
import com.codingshuttle.project.uber.uberApp.dto.WalletTransactionDto;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionService walletTransactionService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#user.getId()")
    public Wallet addMoneyToWallet(User user, Double amount, String transactionId, Ride ride, TransactionMethod transactionMethod) {
        validateAmount(amount);
        Wallet wallet = findByUser(user);
        wallet.setBalance(wallet.getBalance()+amount);

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
    public Wallet deductMoneyFromWallet(User user, Double amount,
                                        String transactionId, Ride ride,
                                        TransactionMethod transactionMethod) {
        validateAmount(amount);
        Wallet wallet = findByUser(user);
        if (wallet.getBalance() < amount) {
            throw new RuntimeConflictException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance()-amount);

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
    public WalletDto addMoneyToMyWallet(Double amount) {
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
    @Transactional
    @CacheEvict(cacheNames = "wallets", key = "#root.target.getCurrentUserIdForCache()")
    public WalletDto withdrawMoneyFromMyWallet(Double amount) {
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

    private void validateAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new RuntimeConflictException("Amount must be greater than 0");
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

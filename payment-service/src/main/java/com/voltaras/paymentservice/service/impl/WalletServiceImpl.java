package com.voltaras.paymentservice.service.impl;

import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.entity.Wallet;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.exception.InsufficientWalletBalanceException;
import com.voltaras.paymentservice.mapper.WalletMapper;
import com.voltaras.paymentservice.repository.WalletRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.UserVerificationService;
import com.voltaras.paymentservice.service.WalletService;
import com.voltaras.paymentservice.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implementation of {@link WalletService}.
 *
 * <p>
 * Wallets are created lazily. Credit and debit lock the wallet row with a
 * pessimistic write lock so concurrent recharges and bill payments are
 * serialized; this is the wallet-specific exception to the otherwise
 * version-less entity convention of the other services.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final PaymentAccessHelper accessHelper;
    private final UserVerificationService userVerificationService;

    @Override
    @Transactional
    public WalletResponse getMyWallet(Long authUserId, String systemRole) {

        accessHelper.requireAuthenticatedUser(authUserId);

        // The user must exist and be active in the Auth Service.
        userVerificationService.verifyActiveUser(authUserId, systemRole);

        // Not read-only: a first access creates and persists the wallet.
        Wallet wallet = getOrCreate(authUserId);

        log.info("Wallet {} of user {} has balance {}",
                wallet.getId(), authUserId, wallet.getBalance());

        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public Wallet credit(Long userId, BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new InsufficientWalletBalanceException(
                    "Credit amount must be greater than zero");
        }

        Wallet wallet = getOrCreateForUpdate(userId);

        wallet.setBalance(MoneyUtils.scale(
                wallet.getBalance().add(MoneyUtils.scale(amount))));

        Wallet saved = walletRepository.save(wallet);

        log.info("Wallet {} credited {} for user {} (new balance {})",
                saved.getId(), amount, userId, saved.getBalance());

        return saved;
    }

    @Override
    @Transactional
    public Wallet debit(Long userId, BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new InsufficientWalletBalanceException(
                    "Debit amount must be greater than zero");
        }

        Wallet wallet = getOrCreateForUpdate(userId);

        BigDecimal scaled = MoneyUtils.scale(amount);

        if (wallet.getBalance().compareTo(scaled) < 0) {

            log.warn("Insufficient wallet balance for user {}: balance {}, "
                    + "required {}", userId, wallet.getBalance(), scaled);

            throw new InsufficientWalletBalanceException(
                    "Insufficient wallet balance. Available: "
                            + wallet.getBalance().toPlainString()
                            + " " + wallet.getCurrency()
                            + ", required: " + scaled.toPlainString()
                            + " " + wallet.getCurrency());
        }

        wallet.setBalance(wallet.getBalance().subtract(scaled));

        Wallet saved = walletRepository.save(wallet);

        log.info("Wallet {} debited {} for user {} (new balance {})",
                saved.getId(), amount, userId, saved.getBalance());

        return saved;
    }

    private Wallet getOrCreate(Long userId) {

        // Read path: a missing wallet is created and persisted lazily.
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(createWallet(userId)));
    }

    private Wallet getOrCreateForUpdate(Long userId) {

        // Write path: the caller (credit/debit) always saves once, so a
        // missing wallet is built here and persisted by that single save.
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createWallet(userId));
    }

    private Wallet createWallet(Long userId) {

        return Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO.setScale(MoneyUtils.SCALE))
                .currency(Currency.INR)
                .build();
    }
}

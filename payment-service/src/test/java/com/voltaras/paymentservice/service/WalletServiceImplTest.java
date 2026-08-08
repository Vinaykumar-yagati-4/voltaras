package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.entity.Wallet;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.exception.InactiveUserException;
import com.voltaras.paymentservice.exception.InsufficientWalletBalanceException;
import com.voltaras.paymentservice.mapper.WalletMapper;
import com.voltaras.paymentservice.repository.WalletRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WalletServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final Long USER_ID = 100L;

    @Mock private WalletRepository walletRepository;
    @Mock private WalletMapper walletMapper;
    @Mock private PaymentAccessHelper accessHelper;
    @Mock private UserVerificationService userVerificationService;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(
                walletRepository, walletMapper, accessHelper,
                userVerificationService);
    }

    @Test
    @DisplayName("Get wallet returns the existing wallet")
    void getMyWallet_existing_returnsBalance() {

        Wallet wallet = buildWallet("1500.00");
        when(walletRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(wallet));
        when(walletMapper.toResponse(wallet))
                .thenReturn(WalletResponse.builder()
                        .userId(USER_ID)
                        .balance(new BigDecimal("1500.00"))
                        .build());

        WalletResponse response = walletService.getMyWallet(
                USER_ID, "CONSUMER");

        assertThat(response.getBalance())
                .isEqualByComparingTo("1500.00");

        // The active auth user was verified before the balance was read.
        verify(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");
    }

    @Test
    @DisplayName("Get wallet creates a zero-balance wallet on first access")
    void getMyWallet_missing_createsWallet() {

        when(walletRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(walletMapper.toResponse(any(Wallet.class)))
                .thenReturn(WalletResponse.builder()
                        .userId(USER_ID)
                        .balance(BigDecimal.ZERO)
                        .build());

        WalletResponse response = walletService.getMyWallet(
                USER_ID, "CONSUMER");

        assertThat(response.getBalance())
                .isEqualByComparingTo("0");

        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Get wallet is rejected for an inactive auth user")
    void getMyWallet_inactiveUser_throws() {

        doThrow(new InactiveUserException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> walletService.getMyWallet(
                USER_ID, "CONSUMER"))
                .isInstanceOf(InactiveUserException.class);

        // The wallet is never read or created.
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Credit adds to the balance")
    void credit_addsToBalance() {

        Wallet wallet = buildWallet("100.00");
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        walletService.credit(USER_ID, new BigDecimal("50.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Credit on a fresh wallet creates it with the credited amount")
    void credit_missingWallet_createsAndCredits() {

        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        walletService.credit(USER_ID, new BigDecimal("500.00"));

        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Debit subtracts from the balance")
    void debit_subtractsFromBalance() {

        Wallet wallet = buildWallet("100.00");
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        walletService.debit(USER_ID, new BigDecimal("40.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Debit beyond the balance raises INSUFFICIENT_WALLET_BALANCE")
    void debit_insufficient_throws() {

        Wallet wallet = buildWallet("10.00");
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() ->
                walletService.debit(USER_ID, new BigDecimal("40.00")))
                .isInstanceOf(InsufficientWalletBalanceException.class)
                .hasMessageContaining("Insufficient wallet balance");

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Debit on a fresh (zero balance) wallet raises INSUFFICIENT_WALLET_BALANCE")
    void debit_missingWallet_throws() {

        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                walletService.debit(USER_ID, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientWalletBalanceException.class);

        // A zero-balance wallet is never persisted on a failed debit.
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Credit or debit with a non-positive amount is rejected")
    void nonPositiveAmounts_rejected() {

        assertThatThrownBy(() ->
                walletService.credit(USER_ID, BigDecimal.ZERO))
                .isInstanceOf(InsufficientWalletBalanceException.class);

        assertThatThrownBy(() ->
                walletService.debit(USER_ID, new BigDecimal("-5.00")))
                .isInstanceOf(InsufficientWalletBalanceException.class);

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    private Wallet buildWallet(String balance) {

        return Wallet.builder()
                .id(1L)
                .userId(USER_ID)
                .balance(new BigDecimal(balance))
                .currency(Currency.INR)
                .build();
    }
}

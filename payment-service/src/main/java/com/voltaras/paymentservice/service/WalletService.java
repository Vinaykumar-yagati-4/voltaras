package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.entity.Wallet;

import java.math.BigDecimal;

/**
 * Wallet operations: read balance, credit (recharge webhook) and debit
 * (bill payment).
 */
public interface WalletService {

    /**
     * Returns the wallet of the authenticated user, creating it with a
     * zero balance on first access. The user is verified against the
     * Auth Service first.
     *
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     * @return wallet state
     */
    WalletResponse getMyWallet(Long authUserId, String systemRole);

    /**
     * Credits the wallet. Called when a recharge webhook reports SUCCESS.
     * The wallet row is pessimistically locked inside the transaction.
     *
     * @param userId wallet owner
     * @param amount amount to add (positive)
     * @return the updated wallet
     */
    Wallet credit(Long userId, BigDecimal amount);

    /**
     * Debits the wallet. Throws
     * {@link com.voltaras.paymentservice.exception.InsufficientWalletBalanceException}
     * when the balance is lower than the amount.
     *
     * @param userId wallet owner
     * @param amount amount to subtract (positive)
     * @return the updated wallet
     */
    Wallet debit(Long userId, BigDecimal amount);
}

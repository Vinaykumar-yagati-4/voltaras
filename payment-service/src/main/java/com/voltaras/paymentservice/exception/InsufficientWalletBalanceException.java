package com.voltaras.paymentservice.exception;

/**
 * Thrown when a bill payment cannot be settled because the wallet balance
 * is lower than the requested amount.
 *
 * <p>
 * Mapped by the {@link GlobalExceptionHandler} to HTTP 400 with the error
 * code {@code INSUFFICIENT_WALLET_BALANCE}.
 * </p>
 */
public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}

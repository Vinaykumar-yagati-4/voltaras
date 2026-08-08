package com.voltaras.paymentservice.enums;

/**
 * Payment method used for a payment transaction.
 *
 * <ul>
 *     <li>{@link #UPI} and {@link #CARD} fund a recharge order through the
 *         Razorpay payment gateway (sandbox/test mode).</li>
 *     <li>{@link #WALLET} is used for bill payments, which are settled
 *         directly from the payer's VOLTARAS wallet balance.</li>
 * </ul>
 *
 * <p>
 * No card numbers, CVV values, UPI PINs or other sensitive payment data are
 * ever accepted or stored by this service.
 * </p>
 */
public enum PaymentMethod {

    /** Unified Payments Interface (UPI), processed via Razorpay. */
    UPI,

    /** Debit / credit card, processed via Razorpay. */
    CARD,

    /** VOLTARAS wallet balance used to pay a bill. */
    WALLET
}

package com.voltaras.paymentservice.enums;

/**
 * Lifecycle status of a payment transaction (recharge or bill payment).
 *
 * <p>
 * Valid transitions are enforced by
 * {@code util.PaymentStatusTransitions}:
 * </p>
 *
 * <ul>
 *     <li>{@link #CREATED} &rarr; {@link #PENDING}, {@link #SUCCESS},
 *         {@link #FAILED}, {@link #CANCELLED}</li>
 *     <li>{@link #PENDING} &rarr; {@link #SUCCESS}, {@link #FAILED},
 *         {@link #CANCELLED}</li>
 *     <li>{@link #SUCCESS} &rarr; {@link #REFUNDED}</li>
 *     <li>{@link #FAILED}, {@link #CANCELLED} and {@link #REFUNDED} are
 *         terminal</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Order created at the gateway; payment not yet attempted. */
    CREATED,

    /** Payment is awaiting gateway confirmation (authorized, not captured). */
    PENDING,

    /** Payment completed successfully. */
    SUCCESS,

    /** Payment failed; a new order is required to try again. */
    FAILED,

    /** Payment was cancelled by the payer before completion. */
    CANCELLED,

    /** Payment was refunded after success (terminal). */
    REFUNDED
}

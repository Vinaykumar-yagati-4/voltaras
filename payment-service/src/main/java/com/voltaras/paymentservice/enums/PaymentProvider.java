package com.voltaras.paymentservice.enums;

/**
 * Payment gateway provider that processed a transaction.
 */
public enum PaymentProvider {

    /**
     * Razorpay payment gateway. Recharge orders are created in Razorpay's
     * sandbox/test mode using the RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET
     * credentials and confirmed through signature-protected webhooks.
     */
    RAZORPAY,

    /**
     * Local development/testing funding: the wallet is credited directly by
     * the VOLTARAS backend without any payment gateway involvement. Used by
     * the wallet top-up endpoint so recharge history records a real,
     * persisted transaction during local development.
     */
    LOCAL
}

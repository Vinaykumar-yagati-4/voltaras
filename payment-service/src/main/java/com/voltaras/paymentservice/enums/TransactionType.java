package com.voltaras.paymentservice.enums;

/**
 * Kind of money movement recorded by the Payment Service.
 */
public enum TransactionType {

    /** Wallet recharge funded through the Razorpay gateway (UPI or CARD). */
    RECHARGE,

    /** Bill payment settled from the wallet balance. */
    BILL_PAYMENT,

    /** Refund credited back to the wallet (future use). */
    REFUND
}

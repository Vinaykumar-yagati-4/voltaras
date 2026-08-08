package com.voltaras.paymentservice.provider;

/**
 * Razorpay order returned by {@code POST /v1/orders}.
 *
 * @param id Razorpay order ID (for example {@code order_xxxxxxxxxxxx})
 * @param amount order amount in paise
 * @param currency ISO currency code (INR)
 * @param status gateway order status (for example {@code created})
 * @param receipt client receipt reference
 */
public record RazorpayOrder(
        String id,
        long amount,
        String currency,
        String status,
        String receipt) {
}

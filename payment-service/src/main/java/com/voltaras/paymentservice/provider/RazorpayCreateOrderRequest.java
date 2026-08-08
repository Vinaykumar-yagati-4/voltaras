package com.voltaras.paymentservice.provider;

import java.util.Map;

/**
 * Payload for {@code POST /v1/orders} on the Razorpay gateway. Amount is
 * always in paise.
 *
 * @param amount order amount in paise
 * @param currency ISO currency code (INR)
 * @param receipt client receipt reference
 * @param notes free-form metadata stored with the order
 */
public record RazorpayCreateOrderRequest(
        long amount,
        String currency,
        String receipt,
        Map<String, String> notes) {
}

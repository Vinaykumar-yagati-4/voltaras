package com.voltaras.paymentservice.provider;

/**
 * Abstraction over the Razorpay payment gateway (sandbox/test mode).
 *
 * <p>
 * The service layer depends only on this interface, never on a concrete
 * provider implementation, so the gateway can be swapped or mocked in
 * automated tests.
 * </p>
 */
public interface RazorpayGatewayClient {

    /**
     * Creates a payment order at Razorpay. Amounts are always expressed in
     * paise when talking to the gateway.
     *
     * @param request order details (amount in paise, INR)
     * @return the Razorpay order created by the gateway
     */
    RazorpayOrder createOrder(RazorpayCreateOrderRequest request);

    /**
     * Verifies the HMAC-SHA256 signature of a webhook payload using the
     * shared webhook secret. Comparison is constant-time.
     *
     * @param payload raw webhook body
     * @param signature signature received in the X-Razorpay-Signature header
     * @param secret configured RAZORPAY_WEBHOOK_SECRET
     * @return true when the signature is valid
     */
    boolean verifyWebhookSignature(
            String payload, String signature, String secret);
}

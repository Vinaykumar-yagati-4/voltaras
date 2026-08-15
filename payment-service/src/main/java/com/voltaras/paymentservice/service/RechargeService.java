package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.dto.request.CreateRechargeOrderRequest;
import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wallet recharge operations backed by the Razorpay gateway.
 */
public interface RechargeService {

    /**
     * Creates a Razorpay order for a wallet recharge (UPI or CARD).
     * Idempotent on the Idempotency-Key header.
     *
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     * @param idempotencyKey client idempotency key
     * @param request recharge details
     * @return the created (or idempotently replayed) recharge order
     */
    RechargeOrderResponse createRechargeOrder(
            Long authUserId, String systemRole, String idempotencyKey,
            CreateRechargeOrderRequest request);

    /**
     * Handles a Razorpay webhook callback after verifying the HMAC-SHA256
     * signature over the raw payload. Successful payments credit the
     * wallet; repeated callbacks are idempotent.
     *
     * @param payload raw webhook body
     * @param signature X-Razorpay-Signature header value
     */
    void handleRazorpayWebhook(String payload, String signature);

    /**
     * Records a local development/testing recharge: credits the wallet
     * directly and persists a {@code SUCCESS} recharge transaction with
     * provider {@code LOCAL} so recharge history reflects it. No payment
     * gateway is involved.
     *
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     * @param organizationId organization the user must actively belong to
     * @param amount amount to credit in INR
     * @return the persisted recharge transaction
     */
    RechargeTransactionResponse recordLocalRecharge(
            Long authUserId, String systemRole, Long organizationId,
            BigDecimal amount);

    /**
     * Returns the recharge history of the authenticated user, newest first.
     * The user is verified against the Auth Service first.
     *
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     * @return recharge transactions
     */
    List<RechargeTransactionResponse> getMyRecharges(
            Long authUserId, String systemRole);
}

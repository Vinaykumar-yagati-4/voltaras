package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.dto.request.PayBillRequest;
import com.voltaras.paymentservice.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Bill payment operations settled from the wallet balance.
 */
public interface PaymentService {

    /**
     * Pays a bill from the wallet balance. The wallet is debited and the
     * Bill Service is notified (PAID or PARTIALLY_PAID) inside one
     * transaction. Idempotent on the Idempotency-Key header.
     *
     * @param authUserId authenticated user ID
     * @param systemRole platform role from X-User-Role
     * @param billId bill ID
     * @param idempotencyKey client idempotency key
     * @param request payment details
     * @return the created (or idempotently replayed) payment
     */
    PaymentResponse payBillFromWallet(
            Long authUserId, String systemRole, Long billId,
            String idempotencyKey, PayBillRequest request);

    /**
     * Returns a payment by ID, only when it belongs to the caller or the
     * caller is a system ADMIN.
     */
    PaymentResponse getPaymentById(
            Long authUserId, String systemRole, Long paymentId);

    /**
     * Returns a payment by its server-generated reference, only when it
     * belongs to the caller or the caller is a system ADMIN.
     */
    PaymentResponse getPaymentByReference(
            Long authUserId, String systemRole, String paymentReference);

    /**
     * Lists the payments of a bill, newest first. The caller must be the
     * bill owner or a system ADMIN.
     */
    List<PaymentResponse> getPaymentsForBill(
            Long authUserId, String systemRole, Long billId);

    /**
     * Lists payments, newest first. Consumers get their own payments;
     * system ADMINs may filter by organization.
     */
    Page<PaymentResponse> getMyPayments(
            Long authUserId, String systemRole,
            Long organizationId, Pageable pageable);
}

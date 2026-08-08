package com.voltaras.paymentservice.client;

import java.math.BigDecimal;

/**
 * Client for the VOLTARAS Bill Service.
 *
 * <p>
 * The repository has no established inter-service communication mechanism
 * (no Feign, WebClient or event bus), so a small isolated HTTP client is
 * used. The implementation lives in {@code client.impl} and is the only
 * place that knows the Bill Service URLs.
 * </p>
 */
public interface BillServiceClient {

    /**
     * Fetches a bill only when it belongs to the given consumer.
     * Bill Service returns 404 for bills that do not exist or are owned by
     * another user, so this both validates existence and ownership.
     *
     * @param billId bill ID
     * @param authUserId authenticated user ID
     * @return bill snapshot
     */
    BillSnapshot getConsumerBill(Long billId, Long authUserId);

    /**
     * Fetches any bill as a system ADMIN.
     *
     * @param billId bill ID
     * @param systemRole platform role from X-User-Role
     * @return bill snapshot
     */
    BillSnapshot getBillAsAdmin(Long billId, String systemRole);

    /**
     * Notifies the Bill Service about a wallet payment. Called only after
     * the wallet has been debited, inside the same transaction.
     *
     * @param billId bill ID
     * @param paymentStatus new payment status to apply: PAID or PARTIALLY_PAID
     * @param cumulativeAmountPaid total amount paid towards the bill so far
     */
    void notifyPaymentStatus(
            Long billId, String paymentStatus, BigDecimal cumulativeAmountPaid);
}

package com.voltaras.billservice.service;

import com.voltaras.billservice.dto.request.CancelBillRequest;
import com.voltaras.billservice.dto.request.GenerateBillRequest;
import com.voltaras.billservice.dto.request.UpdateBillRequest;
import com.voltaras.billservice.dto.request.UpdatePaymentStatusRequest;
import com.voltaras.billservice.dto.response.BillResponse;
import com.voltaras.billservice.dto.response.BillSummaryResponse;
import com.voltaras.billservice.enums.BillStatus;

import java.util.List;

/**
 * Business operations for electricity bills.
 *
 * <p>
 * Identity always comes from the API Gateway headers (X-User-Id /
 * X-User-Role). authUserId is never accepted from request bodies.
 * </p>
 */
public interface BillService {

    // ------------------------------------------------------------------
    // Consumer operations
    // ------------------------------------------------------------------

    /**
     * Full bill history of the authenticated consumer, newest first.
     */
    List<BillSummaryResponse> getMyBills(Long authUserId);

    /**
     * Single bill of the authenticated consumer. Consumers can only
     * access their own bills.
     */
    BillResponse getMyBillById(Long authUserId, Long billId);

    /**
     * Consumer bills for a specific billing month and year.
     */
    List<BillSummaryResponse> getMyBillsByPeriod(
            Long authUserId, Integer month, Integer year);

    /**
     * Consumer bills that are still payable (UNPAID, PARTIALLY_PAID or
     * FAILED, and not CANCELLED).
     */
    List<BillSummaryResponse> getMyOutstandingBills(Long authUserId);

    // ------------------------------------------------------------------
    // Admin operations
    // ------------------------------------------------------------------

    /**
     * Generates a new bill with tariff calculation for the authenticated
     * user identified by X-User-Id.
     */
    BillResponse generateBill(
            Long authUserId, String systemRole, GenerateBillRequest request);

    /**
     * All bills with optional status / month / year filters.
     */
    List<BillSummaryResponse> getAllBills(
            String systemRole, BillStatus status, Integer month, Integer year);

    /**
     * Bill details by ID (admin view).
     */
    BillResponse getBillById(String systemRole, Long billId);

    /**
     * Updates due date, late fee, discount and remarks, then
     * recalculates the total amount.
     */
    BillResponse updateBill(
            String systemRole, Long billId, UpdateBillRequest request);

    /**
     * Cancels a bill. PAID bills cannot be cancelled.
     */
    BillResponse cancelBill(
            String systemRole, Long billId, CancelBillRequest request);

    /**
     * Updates the payment state of a bill. CANCELLED bills cannot be
     * paid later.
     */
    BillResponse updatePaymentStatus(
            String systemRole, Long billId, UpdatePaymentStatusRequest request);

    /**
     * Marks eligible bills as OVERDUE, applies the late fee and returns
     * the number of bills updated.
     */
    int markOverdue(String systemRole);
}

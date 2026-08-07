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
 * Defines operations for consumers and administrators to manage electricity bills.
 */
public interface BillService {

    List<BillSummaryResponse> getMyBills(Long authUserId);

    BillResponse getMyBillById(Long authUserId, Long billId);

    List<BillSummaryResponse> getMyBillsByPeriod(
            Long authUserId,
            Integer month,
            Integer year
    );

    List<BillSummaryResponse> getMyOutstandingBills(Long authUserId);

    BillResponse generateBill(
            Long adminUserId,
            String systemRole,
            GenerateBillRequest request
    );

    List<BillSummaryResponse> getAllBills(
            String systemRole,
            BillStatus status,
            Integer month,
            Integer year
    );

    BillResponse getBillById(
            String systemRole,
            Long billId
    );

    BillResponse updateBill(
            String systemRole,
            Long billId,
            UpdateBillRequest request
    );

    BillResponse cancelBill(
            String systemRole,
            Long billId,
            CancelBillRequest request
    );

    BillResponse updatePaymentStatus(
            String systemRole,
            Long billId,
            UpdatePaymentStatusRequest request
    );

    int markOverdue(String systemRole);
}
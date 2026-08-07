package com.voltaras.billservice.service.impl;

import com.voltaras.billservice.dto.request.CancelBillRequest;
import com.voltaras.billservice.dto.request.GenerateBillRequest;
import com.voltaras.billservice.dto.request.UpdateBillRequest;
import com.voltaras.billservice.dto.request.UpdatePaymentStatusRequest;
import com.voltaras.billservice.dto.response.BillResponse;
import com.voltaras.billservice.dto.response.BillSummaryResponse;
import com.voltaras.billservice.entity.Bill;
import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.enums.PaymentStatus;
import com.voltaras.billservice.exception.BusinessRuleException;
import com.voltaras.billservice.exception.DuplicateResourceException;
import com.voltaras.billservice.exception.ResourceNotFoundException;
import com.voltaras.billservice.mapper.BillMapper;
import com.voltaras.billservice.repository.BillRepository;
import com.voltaras.billservice.security.BillAccessHelper;
import com.voltaras.billservice.service.BillService;
import com.voltaras.billservice.util.BillCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Implementation of {@link BillService}.
 *
 * <p>
 * Business rules enforced here:
 * </p>
 *
 * <ul>
 *     <li>current reading &gt;= previous reading</li>
 *     <li>billing month between 1 and 12, billing year valid</li>
 *     <li>due date after generated date</li>
 *     <li>no duplicate bill per consumer / meter / month / year</li>
 *     <li>consumers only access their own bills</li>
 *     <li>admin operations require X-User-Role = ADMIN</li>
 *     <li>PAID bills cannot be cancelled</li>
 *     <li>CANCELLED bills cannot be paid later</li>
 *     <li>all money uses scale 2 and RoundingMode.HALF_UP</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillServiceImpl implements BillService {

    /** Statuses that are still payable and therefore overdue-eligible. */
    private static final List<BillStatus> OVERDUE_ACTIVE_STATUSES =
            List.of(BillStatus.GENERATED, BillStatus.PENDING);

    /** Payment statuses that count as settled (never overdue-eligible). */
    private static final List<PaymentStatus> SETTLED_PAYMENT_STATUSES =
            List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED);

    /** Payment statuses a consumer cannot consider "outstanding". */
    private static final List<PaymentStatus> SETTLED_FOR_OUTSTANDING =
            List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED);

    private static final int MIN_VALID_YEAR = 2000;

    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final BillAccessHelper accessHelper;

    // ==================================================================
    // Consumer operations
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BillSummaryResponse> getMyBills(Long authUserId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        List<Bill> bills =
                billRepository.findByAuthUserIdOrderByCreatedAtDesc(authUserId);

        log.info("Consumer {} fetched {} bills", authUserId, bills.size());

        return bills.stream()
                .map(billMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getMyBillById(Long authUserId, Long billId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        Bill bill = billRepository.findByIdAndAuthUserId(billId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bill", "id", billId));

        return billMapper.toResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillSummaryResponse> getMyBillsByPeriod(
            Long authUserId, Integer month, Integer year) {

        accessHelper.requireAuthenticatedUser(authUserId);

        validateBillingPeriod(month, year);

        List<Bill> bills =
                billRepository
                        .findByAuthUserIdAndBillingMonthAndBillingYearOrderByCreatedAtDesc(
                                authUserId, month, year);

        log.info("Consumer {} fetched {} bills for {}-{}",
                authUserId, bills.size(), year, month);

        return bills.stream()
                .map(billMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillSummaryResponse> getMyOutstandingBills(Long authUserId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        List<Bill> bills = billRepository.findOutstandingBillsByAuthUserId(
                authUserId,
                BillStatus.CANCELLED,
                SETTLED_FOR_OUTSTANDING
        );

        log.info("Consumer {} has {} outstanding bills", authUserId, bills.size());

        return bills.stream()
                .map(billMapper::toSummary)
                .toList();
    }

    // ==================================================================
    // Admin operations
    // ==================================================================

    @Override
    @Transactional
    public BillResponse generateBill(
            Long adminUserId, String systemRole, GenerateBillRequest request) {

        accessHelper.requireAuthenticatedUser(adminUserId);
        accessHelper.requireSystemAdmin(systemRole);

        Long consumerUserId = request.getAuthUserId();

        BigDecimal previousReading = request.getPreviousReading();
        BigDecimal currentReading = request.getCurrentReading();

        validateReadingValues(previousReading, currentReading);
        validateBillingPeriod(
                request.getBillingMonth(), request.getBillingYear());

        LocalDate generatedDate = request.getGeneratedDate() != null
                ? request.getGeneratedDate()
                : LocalDate.now();

        validateDueDate(request.getDueDate(), generatedDate);

        checkDuplicateBill(
                consumerUserId,
                request.getMeterNumber(),
                request.getBillingMonth(),
                request.getBillingYear()
        );

        // Centralized tariff calculation.
        BigDecimal unitsConsumed =
                BillCalculator.calculateUnitsConsumed(previousReading, currentReading);

        BigDecimal energyCharge =
                BillCalculator.calculateEnergyCharge(unitsConsumed);

        BigDecimal fixedCharge = BillCalculator.FIXED_CHARGE;

        BigDecimal taxAmount =
                BillCalculator.calculateTaxAmount(energyCharge, fixedCharge);

        BigDecimal lateFee = BillCalculator.ZERO;

        BigDecimal totalAmount = BillCalculator.calculateTotalAmount(
                energyCharge, fixedCharge, taxAmount, lateFee);

        Bill bill = Bill.builder()
                .authUserId(consumerUserId)
                .meterReadingId(request.getMeterReadingId())
                .meterNumber(request.getMeterNumber())
                .billingMonth(request.getBillingMonth())
                .billingYear(request.getBillingYear())
                .previousReading(previousReading)
                .currentReading(currentReading)
                .unitsConsumed(unitsConsumed)
                .energyCharge(energyCharge)
                .fixedCharge(fixedCharge)
                .taxAmount(taxAmount)
                .lateFee(lateFee)
                .totalAmount(totalAmount)
                .amountPaid(BillCalculator.ZERO)
                .outstandingAmount(totalAmount)
                .billStatus(BillStatus.GENERATED)
                .paymentStatus(PaymentStatus.UNPAID)
                .generatedDate(generatedDate)
                .dueDate(request.getDueDate())
                .remarks(request.getRemarks())
                .generatedBy(adminUserId)
                .build();

        Bill saved = billRepository.save(bill);

        log.info("Admin {} generated bill {} for consumer {} (month {}-{}, total {})",
                adminUserId, saved.getId(), saved.getAuthUserId(),
                saved.getBillingMonth(), saved.getBillingYear(), saved.getTotalAmount());

        return billMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillSummaryResponse> getAllBills(
            String systemRole, BillStatus status, Integer month, Integer year) {

        accessHelper.requireSystemAdmin(systemRole);

        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessRuleException(
                    "Billing month must be between 1 and 12");
        }

        List<Bill> bills =
                billRepository.findAdminFiltered(status, month, year);

        log.info("Admin {} fetched {} bills (status={}, month={}, year={})",
                systemRole, bills.size(), status, month, year);

        return bills.stream()
                .map(billMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillById(String systemRole, Long billId) {

        accessHelper.requireSystemAdmin(systemRole);

        Bill bill = requireBill(billId);

        return billMapper.toResponse(bill);
    }

    @Override
    @Transactional
    public BillResponse updateBill(
            String systemRole, Long billId, UpdateBillRequest request) {

        accessHelper.requireSystemAdmin(systemRole);

        Bill bill = requireBill(billId);

        requireModifiable(bill);

        if (request.getDueDate() != null
                && !request.getDueDate().isAfter(bill.getGeneratedDate())) {

            throw new BusinessRuleException(
                    "Due date must be after the generated date");
        }

        // Null-safe: fields omitted from the request stay unchanged.
        billMapper.updateBill(request, bill);

        // Late fee may have changed, so recalculate money fields.
        recalculateTotals(bill);

        Bill saved = billRepository.save(bill);

        log.info("Admin {} updated bill {}", systemRole, saved.getId());

        return billMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BillResponse cancelBill(
            String systemRole, Long billId, CancelBillRequest request) {

        accessHelper.requireSystemAdmin(systemRole);

        Bill bill = requireBill(billId);

        if (bill.getBillStatus() == BillStatus.PAID) {
            throw new BusinessRuleException(
                    "PAID bills cannot be cancelled");
        }

        if (bill.getBillStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Bill is already cancelled");
        }

        bill.setBillStatus(BillStatus.CANCELLED);

        String reason = "CANCELLED: " + request.getReason();
        bill.setRemarks(
                bill.getRemarks() == null || bill.getRemarks().isBlank()
                        ? reason
                        : bill.getRemarks() + " | " + reason);

        Bill saved = billRepository.save(bill);

        log.info("Admin {} cancelled bill {}", systemRole, saved.getId());

        return billMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BillResponse updatePaymentStatus(
            String systemRole, Long billId, UpdatePaymentStatusRequest request) {

        accessHelper.requireSystemAdmin(systemRole);

        Bill bill = requireBill(billId);

        if (bill.getBillStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "CANCELLED bills cannot be paid or updated");
        }

        PaymentStatus targetStatus = request.getPaymentStatus();

        if (targetStatus == null) {
            throw new BusinessRuleException(
                    "paymentStatus is required");
        }

        switch (targetStatus) {

            case PAID -> applyFullPayment(bill, request);
            case PARTIALLY_PAID -> applyPartialPayment(bill, request);
            case UNPAID -> resetPayment(bill, request);
            case FAILED -> applyFailedPayment(bill, request);
            case REFUNDED -> applyRefund(bill, request);
        }

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            bill.setRemarks(request.getRemarks());
        }

        Bill saved = billRepository.save(bill);

        log.info("Admin {} set payment status of bill {} to {}",
                systemRole, saved.getId(), saved.getPaymentStatus());

        return billMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public int markOverdue(String systemRole) {

        accessHelper.requireSystemAdmin(systemRole);

        LocalDate today = LocalDate.now();

        List<Bill> candidates = billRepository.findOverdueCandidates(
                OVERDUE_ACTIVE_STATUSES,
                SETTLED_PAYMENT_STATUSES,
                today
        );

        int updated = 0;

        for (Bill bill : candidates) {

            bill.setBillStatus(BillStatus.OVERDUE);
            bill.setLateFee(BillCalculator.LATE_FEE);

            recalculateTotals(bill);

            // Entities are managed inside the transaction; the changes are
            // flushed automatically at commit (no explicit per-bill save).
            updated++;
        }

        log.info("Admin {} marked {} bills as OVERDUE", systemRole, updated);

        return updated;
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    /**
     * Recalculates total and outstanding amounts from the current entity
     * values after a late-fee change.
     */
    private void recalculateTotals(Bill bill) {

        BigDecimal totalAmount = BillCalculator.calculateTotalAmount(
                bill.getEnergyCharge(),
                bill.getFixedCharge(),
                bill.getTaxAmount(),
                bill.getLateFee()
        );

        bill.setTotalAmount(totalAmount);

        BigDecimal outstandingAmount =
                BillCalculator.calculateOutstandingAmount(
                        totalAmount, bill.getAmountPaid());

        bill.setOutstandingAmount(outstandingAmount);
    }

    private void applyFullPayment(Bill bill, UpdatePaymentStatusRequest request) {

        if (bill.getPaymentStatus() == PaymentStatus.PAID
                && bill.getBillStatus() == BillStatus.PAID) {

            throw new BusinessRuleException(
                    "Bill is already fully paid");
        }

        BigDecimal amountPaid = request.getAmountPaid() != null
                ? BillCalculator.scaleMoney(request.getAmountPaid())
                : bill.getTotalAmount();

        if (amountPaid.compareTo(bill.getTotalAmount()) < 0) {

            throw new BusinessRuleException(
                    "amountPaid must be at least totalAmount to mark the bill as PAID");
        }

        bill.setAmountPaid(amountPaid);
        bill.setOutstandingAmount(BillCalculator.ZERO);
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.setBillStatus(BillStatus.PAID);
        bill.setPaidAt(request.getPaidAt() != null
                ? request.getPaidAt()
                : LocalDateTime.now());
    }

    private void applyPartialPayment(Bill bill, UpdatePaymentStatusRequest request) {

        BigDecimal amountPaid = request.getAmountPaid();

        if (amountPaid == null) {
            throw new BusinessRuleException(
                    "amountPaid is required for PARTIALLY_PAID status");
        }

        amountPaid = BillCalculator.scaleMoney(amountPaid);

        if (amountPaid.signum() <= 0) {
            throw new BusinessRuleException(
                    "amountPaid must be greater than zero");
        }

        if (amountPaid.compareTo(bill.getTotalAmount()) >= 0) {
            throw new BusinessRuleException(
                    "amountPaid must be less than totalAmount for PARTIALLY_PAID status");
        }

        bill.setAmountPaid(amountPaid);
        bill.setOutstandingAmount(
                BillCalculator.calculateOutstandingAmount(
                        bill.getTotalAmount(), amountPaid));
        bill.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        bill.setPaidAt(request.getPaidAt() != null
                ? request.getPaidAt()
                : LocalDateTime.now());
    }

    private void resetPayment(Bill bill, UpdatePaymentStatusRequest request) {

        if (bill.getBillStatus() == BillStatus.PAID) {
            throw new BusinessRuleException(
                    "A fully PAID bill cannot be reverted to UNPAID");
        }

        bill.setAmountPaid(BillCalculator.ZERO);
        bill.setOutstandingAmount(bill.getTotalAmount());
        bill.setPaymentStatus(PaymentStatus.UNPAID);
        bill.setPaidAt(null);
    }

    private void applyFailedPayment(Bill bill, UpdatePaymentStatusRequest request) {

        bill.setPaymentStatus(PaymentStatus.FAILED);
        bill.setPaidAt(null);
    }

    private void applyRefund(Bill bill, UpdatePaymentStatusRequest request) {

        bill.setPaymentStatus(PaymentStatus.REFUNDED);
        bill.setAmountPaid(BillCalculator.ZERO);
        bill.setOutstandingAmount(bill.getTotalAmount());
        bill.setPaidAt(null);
    }

    private Bill requireBill(Long billId) {

        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bill", "id", billId));
    }

    /**
     * PAID and CANCELLED bills are final and cannot be modified.
     */
    private void requireModifiable(Bill bill) {

        if (bill.getBillStatus() == BillStatus.PAID) {
            throw new BusinessRuleException(
                    "PAID bills cannot be modified");
        }

        if (bill.getBillStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "CANCELLED bills cannot be modified");
        }
    }

    /**
     * Current reading must be &gt;= previous reading; both must be
     * non-negative.
     */
    private void validateReadingValues(
            BigDecimal previousReading, BigDecimal currentReading) {

        if (previousReading == null || currentReading == null) {
            throw new BusinessRuleException(
                    "Both previous and current readings are required");
        }

        if (previousReading.signum() < 0 || currentReading.signum() < 0) {
            throw new BusinessRuleException(
                    "Meter readings must not be negative");
        }

        if (currentReading.compareTo(previousReading) < 0) {

            throw new BusinessRuleException(
                    "Current reading must be greater than or equal to previous reading");
        }
    }

    /**
     * Billing month must be 1-12 and the billing year must be within a
     * valid range (2000 .. current year + 1).
     */
    private void validateBillingPeriod(Integer month, Integer year) {

        if (month == null || month < 1 || month > 12) {
            throw new BusinessRuleException(
                    "Billing month must be between 1 and 12");
        }

        if (year == null || year < MIN_VALID_YEAR
                || year > Year.now().getValue() + 1) {

            throw new BusinessRuleException(
                    "Billing year is invalid");
        }
    }

    private void validateDueDate(LocalDate dueDate, LocalDate generatedDate) {

        if (dueDate == null) {
            throw new BusinessRuleException(
                    "Due date is required");
        }

        if (!dueDate.isAfter(generatedDate)) {
            throw new BusinessRuleException(
                    "Due date must be after the generated date");
        }
    }

    private void checkDuplicateBill(
            Long authUserId,
            String meterNumber,
            Integer billingMonth,
            Integer billingYear
    ) {

        boolean exists =
                billRepository.existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
                        authUserId, meterNumber, billingMonth, billingYear);

        if (exists) {

            throw new DuplicateResourceException(
                    "Bill",
                    "authUserId/meterNumber/billingMonth/billingYear",
                    authUserId + " / " + meterNumber + " / "
                            + billingMonth + " / " + billingYear
            );
        }
    }
}
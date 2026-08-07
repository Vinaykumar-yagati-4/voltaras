package com.voltaras.billservice.service;

import com.voltaras.billservice.dto.request.CancelBillRequest;
import com.voltaras.billservice.dto.request.GenerateBillRequest;
import com.voltaras.billservice.dto.request.UpdateBillRequest;
import com.voltaras.billservice.dto.request.UpdatePaymentStatusRequest;
import com.voltaras.billservice.dto.response.BillResponse;
import com.voltaras.billservice.entity.Bill;
import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.enums.PaymentStatus;
import com.voltaras.billservice.exception.BusinessRuleException;
import com.voltaras.billservice.exception.DuplicateResourceException;
import com.voltaras.billservice.exception.ForbiddenOperationException;
import com.voltaras.billservice.exception.ResourceNotFoundException;
import com.voltaras.billservice.mapper.BillMapper;
import com.voltaras.billservice.repository.BillRepository;
import com.voltaras.billservice.security.BillAccessHelper;
import com.voltaras.billservice.service.impl.BillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BillServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class BillServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long ADMIN_ID = 1L;
    private static final Long BILL_ID = 5L;

    @Mock private BillRepository billRepository;
    @Mock private BillMapper billMapper;
    @Mock private BillAccessHelper accessHelper;

    private BillServiceImpl billService;

    @BeforeEach
    void setUp() {
        billService = new BillServiceImpl(billRepository, billMapper, accessHelper);
    }

    // ------------------------------------------------------------------
    // Generate bill
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Generate: success, calculates units, energy, tax and total")
    void generateBill_success_calculatesAmounts() {

        GenerateBillRequest request = GenerateBillRequest.builder()
                .authUserId(USER_ID)
                .meterReadingId(42L)
                .meterNumber("MTR-2024-00123")
                .previousReading(new BigDecimal("1250.50"))
                .currentReading(new BigDecimal("1385.75"))
                .billingMonth(6)
                .billingYear(2026)
                .generatedDate(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 6, 16))
                .build();

        when(billRepository
                .existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
                        USER_ID, "MTR-2024-00123", 6, 2026))
                .thenReturn(false);

        when(billRepository.save(any(Bill.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(billMapper.toResponse(any(Bill.class)))
                .thenReturn(BillResponse.builder().id(BILL_ID).build());

        BillResponse response = billService.generateBill(USER_ID, "ADMIN", request);

        assertThat(response.getId()).isEqualTo(BILL_ID);

        verify(billRepository).save(org.mockito.ArgumentMatchers.argThat(saved -> {

            // units = 1385.75 - 1250.50 = 135.25
            assertThat(saved.getUnitsConsumed()).isEqualByComparingTo(new BigDecimal("135.25"));

            // energy = 100*1.50 + 35.25*2.50 = 238.125 -> 238.13 (half up)
            assertThat(saved.getEnergyCharge()).isEqualByComparingTo(new BigDecimal("238.13"));

            assertThat(saved.getFixedCharge()).isEqualByComparingTo(new BigDecimal("100.00"));

            // tax = (238.13 + 100) * 0.05 = 16.9065 -> 16.91
            assertThat(saved.getTaxAmount()).isEqualByComparingTo(new BigDecimal("16.91"));

            // total = 238.13 + 100 + 16.91 = 355.04
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("355.04"));
            assertThat(saved.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("355.04"));

            assertThat(saved.getAuthUserId()).isEqualTo(USER_ID);
            assertThat(saved.getGeneratedBy()).isEqualTo(USER_ID);
            assertThat(saved.getBillStatus()).isEqualTo(BillStatus.GENERATED);
            assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(saved.getAmountPaid()).isEqualByComparingTo(new BigDecimal("0.00"));

            return true;
        }));
    }

    @Test
    @DisplayName("Generate: duplicate bill for same user/meter/period rejected")
    void generateBill_duplicate_throwsDuplicateResourceException() {

        GenerateBillRequest request = validRequest();

        when(billRepository
                .existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
                        USER_ID, "MTR-2024-00123", 6, 2026))
                .thenReturn(true);

        assertThatThrownBy(() -> billService.generateBill(USER_ID, "ADMIN", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    @DisplayName("Generate: current reading below previous reading rejected")
    void generateBill_currentBelowPrevious_throwsBusinessRule() {

        GenerateBillRequest request = validRequest();
        request.setCurrentReading(new BigDecimal("1200.00"));
        request.setPreviousReading(new BigDecimal("1250.50"));

        assertThatThrownBy(() -> billService.generateBill(USER_ID, "ADMIN", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Current reading must be greater than or equal to previous reading");

        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    @DisplayName("Generate: invalid billing month rejected")
    void generateBill_invalidMonth_throwsBusinessRule() {

        GenerateBillRequest request = validRequest();
        request.setBillingMonth(13);

        assertThatThrownBy(() -> billService.generateBill(USER_ID, "ADMIN", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Billing month must be between 1 and 12");
    }

    @Test
    @DisplayName("Generate: due date not after generated date rejected")
    void generateBill_dueDateNotAfterGenerated_throwsBusinessRule() {

        GenerateBillRequest request = validRequest();
        request.setDueDate(LocalDate.of(2026, 6, 1));
        request.setGeneratedDate(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> billService.generateBill(USER_ID, "ADMIN", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Due date must be after the generated date");
    }

    @Test
    @DisplayName("Generate: non-admin caller rejected")
    void generateBill_nonAdmin_throwsForbidden() {

        doThrow(new ForbiddenOperationException(
                "Only system ADMIN users can perform this operation"))
                .when(accessHelper).requireSystemAdmin("CONSUMER");

        assertThatThrownBy(() -> billService.generateBill(USER_ID, "CONSUMER", validRequest()))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(billRepository, never()).save(any(Bill.class));
    }

    // ------------------------------------------------------------------
    // Consumer ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("My bill by id: consumer can access own bill")
    void getMyBillById_owned_returnsBill() {

        Bill bill = buildBill();
        when(billRepository.findByIdAndAuthUserId(BILL_ID, USER_ID))
                .thenReturn(Optional.of(bill));
        when(billMapper.toResponse(bill)).thenReturn(BillResponse.builder().id(BILL_ID).build());

        BillResponse response = billService.getMyBillById(USER_ID, BILL_ID);

        assertThat(response.getId()).isEqualTo(BILL_ID);
    }

    @Test
    @DisplayName("My bill by id: another user's bill is not exposed")
    void getMyBillById_notOwned_throwsNotFound() {

        when(billRepository.findByIdAndAuthUserId(BILL_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> billService.getMyBillById(USER_ID, BILL_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bill not found");
    }

    @Test
    @DisplayName("My bills: returns only the consumer's history")
    void getMyBills_returnsOwnBills() {

        Bill bill = buildBill();
        when(billRepository.findByAuthUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(bill));
        when(billMapper.toSummary(bill)).thenReturn(null);

        assertThat(billService.getMyBills(USER_ID)).hasSize(1);
    }

    @Test
    @DisplayName("Outstanding bills: settled and cancelled bills excluded")
    void getMyOutstandingBills_excludesSettled() {

        Bill bill = buildBill();
        when(billRepository.findOutstandingBillsByAuthUserId(
                eq(USER_ID), eq(BillStatus.CANCELLED), anyList()))
                .thenReturn(List.of(bill));
        when(billMapper.toSummary(bill)).thenReturn(null);

        assertThat(billService.getMyOutstandingBills(USER_ID)).hasSize(1);
    }

    @Test
    @DisplayName("My bills by period: invalid month rejected")
    void getMyBillsByPeriod_invalidMonth_throwsBusinessRule() {

        assertThatThrownBy(() -> billService.getMyBillsByPeriod(USER_ID, 0, 2026))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Billing month must be between 1 and 12");
    }

    // ------------------------------------------------------------------
    // Cancel
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cancel: PAID bill cannot be cancelled")
    void cancelBill_paidBill_throwsBusinessRule() {

        Bill bill = buildBill();
        bill.setBillStatus(BillStatus.PAID);
        bill.setPaymentStatus(PaymentStatus.PAID);

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billService.cancelBill(
                "ADMIN", BILL_ID, new CancelBillRequest("Incorrect reading")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAID bills cannot be cancelled");

        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    @DisplayName("Cancel: unpaid bill is cancelled with reason")
    void cancelBill_unpaidBill_success() {

        Bill bill = buildBill();

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billMapper.toResponse(any(Bill.class)))
                .thenReturn(BillResponse.builder().id(BILL_ID).build());

        BillResponse response = billService.cancelBill(
                "ADMIN", BILL_ID, new CancelBillRequest("Incorrect reading"));

        assertThat(response.getId()).isEqualTo(BILL_ID);
        assertThat(bill.getBillStatus()).isEqualTo(BillStatus.CANCELLED);
        assertThat(bill.getRemarks()).contains("Incorrect reading");
    }

    // ------------------------------------------------------------------
    // Payment status
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Payment: CANCELLED bill cannot be paid later")
    void updatePaymentStatus_cancelled_throwsBusinessRule() {

        Bill bill = buildBill();
        bill.setBillStatus(BillStatus.CANCELLED);

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));

        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                .paymentStatus(PaymentStatus.PAID)
                .amountPaid(new BigDecimal("355.04"))
                .build();

        assertThatThrownBy(() -> billService.updatePaymentStatus("ADMIN", BILL_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED bills cannot be paid or updated");
    }

    @Test
    @DisplayName("Payment: full payment settles the bill")
    void updatePaymentStatus_fullPayment_success() {

        Bill bill = buildBill();

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billMapper.toResponse(any(Bill.class)))
                .thenReturn(BillResponse.builder().id(BILL_ID).build());

        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                .paymentStatus(PaymentStatus.PAID)
                .amountPaid(new BigDecimal("355.04"))
                .remarks("UPI payment received")
                .build();

        billService.updatePaymentStatus("ADMIN", BILL_ID, request);

        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(bill.getBillStatus()).isEqualTo(BillStatus.PAID);
        assertThat(bill.getRemarks()).isEqualTo("UPI payment received");
        assertThat(bill.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(bill.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Payment: amount below total cannot be marked PAID")
    void updatePaymentStatus_shortAmount_throwsBusinessRule() {

        Bill bill = buildBill();

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));

        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                .paymentStatus(PaymentStatus.PAID)
                .amountPaid(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> billService.updatePaymentStatus("ADMIN", BILL_ID, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("amountPaid must be at least totalAmount");
    }

    @Test
    @DisplayName("Payment: partial payment keeps the bill outstanding")
    void updatePaymentStatus_partialPayment_success() {

        Bill bill = buildBill();

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billMapper.toResponse(any(Bill.class)))
                .thenReturn(BillResponse.builder().id(BILL_ID).build());

        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                .paymentStatus(PaymentStatus.PARTIALLY_PAID)
                .amountPaid(new BigDecimal("100.00"))
                .build();

        billService.updatePaymentStatus("ADMIN", BILL_ID, request);

        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(bill.getBillStatus()).isEqualTo(BillStatus.GENERATED);
        assertThat(bill.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("255.04"));
    }

    // ------------------------------------------------------------------
    // Update bill
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Update: late fee change recalculates the total amount")
    void updateBill_lateFeeChange_recalculatesTotal() {

        Bill bill = buildBill();

        UpdateBillRequest request = UpdateBillRequest.builder()
                .lateFee(new BigDecimal("50.00"))
                .build();

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billMapper.toResponse(any(Bill.class)))
                .thenReturn(BillResponse.builder().id(BILL_ID).build());

        // Simulate the real MapStruct mapper applying the non-null lateFee.
        bill.setLateFee(request.getLateFee());

        billService.updateBill("ADMIN", BILL_ID, request);

        // 355.04 + 50.00 late fee
        assertThat(bill.getTotalAmount()).isEqualByComparingTo(new BigDecimal("405.04"));
        assertThat(bill.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("405.04"));
    }

    @Test
    @DisplayName("Update: PAID bill cannot be modified")
    void updateBill_paidBill_throwsBusinessRule() {

        Bill bill = buildBill();
        bill.setBillStatus(BillStatus.PAID);
        bill.setPaymentStatus(PaymentStatus.PAID);

        when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billService.updateBill(
                "ADMIN", BILL_ID, new UpdateBillRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAID bills cannot be modified");
    }

    // ------------------------------------------------------------------
    // Mark overdue
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Overdue: eligible bills marked OVERDUE with late fee applied")
    void markOverdue_appliesLateFeeAndRecalculates() {

        Bill bill = buildBill();

        when(billRepository.findOverdueCandidates(anyList(), anyList(), any(LocalDate.class)))
                .thenReturn(List.of(bill));

        int updated = billService.markOverdue("ADMIN");

        assertThat(updated).isEqualTo(1);
        assertThat(bill.getBillStatus()).isEqualTo(BillStatus.OVERDUE);
        assertThat(bill.getLateFee()).isEqualByComparingTo(new BigDecimal("50.00"));
        // 355.04 + 50.00 late fee
        assertThat(bill.getTotalAmount()).isEqualByComparingTo(new BigDecimal("405.04"));
        assertThat(bill.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("405.04"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private GenerateBillRequest validRequest() {
        return GenerateBillRequest.builder()
                .authUserId(USER_ID)
                .meterReadingId(42L)
                .meterNumber("MTR-2024-00123")
                .previousReading(new BigDecimal("1250.50"))
                .currentReading(new BigDecimal("1385.75"))
                .billingMonth(6)
                .billingYear(2026)
                .generatedDate(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 6, 16))
                .build();
    }

    private Bill buildBill() {
        return Bill.builder()
                .id(BILL_ID)
                .authUserId(USER_ID)
                .meterReadingId(42L)
                .meterNumber("MTR-2024-00123")
                .billingMonth(6)
                .billingYear(2026)
                .previousReading(new BigDecimal("1250.50"))
                .currentReading(new BigDecimal("1385.75"))
                .unitsConsumed(new BigDecimal("135.25"))
                .energyCharge(new BigDecimal("238.13"))
                .fixedCharge(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("16.91"))
                .lateFee(new BigDecimal("0.00"))
                .discountAmount(new BigDecimal("0.00"))
                .totalAmount(new BigDecimal("355.04"))
                .amountPaid(new BigDecimal("0.00"))
                .outstandingAmount(new BigDecimal("355.04"))
                .billStatus(BillStatus.GENERATED)
                .paymentStatus(PaymentStatus.UNPAID)
                .generatedDate(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 6, 16))
                .generatedBy(ADMIN_ID)
                .build();
    }
}

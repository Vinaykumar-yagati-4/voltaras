package com.voltaras.paymentservice.service;

import com.voltaras.paymentservice.client.BillServiceClient;
import com.voltaras.paymentservice.client.BillSnapshot;
import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.dto.request.PayBillRequest;
import com.voltaras.paymentservice.dto.response.PaymentResponse;
import com.voltaras.paymentservice.entity.PaymentTransaction;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.enums.TransactionType;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.exception.BusinessRuleException;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.IdempotencyConflictException;
import com.voltaras.paymentservice.exception.InactiveUserException;
import com.voltaras.paymentservice.exception.InsufficientWalletBalanceException;
import com.voltaras.paymentservice.exception.ResourceNotFoundException;
import com.voltaras.paymentservice.exception.UpstreamServiceException;
import com.voltaras.paymentservice.exception.UserNotFoundException;
import com.voltaras.paymentservice.mapper.PaymentMapper;
import com.voltaras.paymentservice.repository.PaymentTransactionRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.impl.PaymentServiceImpl;
import com.voltaras.paymentservice.util.PaymentReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentServiceImpl} (wallet-funded bill payments).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long BILL_ID = 5L;
    private static final Long ORG_ID = 6L;
    private static final Long PAYMENT_ID = 1L;
    private static final String IDEMPOTENCY_KEY = "pay-bill-5-2026-08-08";
    private static final BigDecimal BILL_TOTAL = new BigDecimal("355.04");

    @Mock private PaymentTransactionRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentAccessHelper accessHelper;
    @Mock private WalletService walletService;
    @Mock private BillServiceClient billServiceClient;
    @Mock private OrganizationServiceClient organizationServiceClient;
    @Mock private UserVerificationService userVerificationService;
    @Mock private PaymentReferenceGenerator paymentReferenceGenerator;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentMapper,
                accessHelper,
                walletService,
                billServiceClient,
                organizationServiceClient,
                userVerificationService,
                paymentReferenceGenerator);
    }

    // ==================================================================
    // Pay bill from wallet
    // ==================================================================

    @Test
    @DisplayName("Pay: full payment debits the wallet and notifies PAID")
    void payBill_fullPayment_success() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));
        when(paymentReferenceGenerator.generate())
                .thenReturn("PAY-ABC123");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(PaymentTransaction.class)))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        PaymentResponse response = paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest());

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);

        verify(walletService).debit(USER_ID, BILL_TOTAL);
        verify(billServiceClient).notifyPaymentStatus(
                BILL_ID, "PAID", BILL_TOTAL);
        verify(organizationServiceClient)
                .requireOrganizationAccess(ORG_ID, USER_ID, "CONSUMER");

        // The bill payment succeeds only when the auth user is verified
        // active AND the organization membership is valid.
        verify(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        ArgumentCaptor<PaymentTransaction> captor =
                ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());

        PaymentTransaction saved = captor.getValue();
        assertThat(saved.getPaymentReference()).isEqualTo("PAY-ABC123");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.WALLET);
        assertThat(saved.getTransactionType())
                .isEqualTo(TransactionType.BILL_PAYMENT);
        assertThat(saved.getPaidAt()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Pay: partial payment on a PARTIALLY_PAID bill notifies PARTIALLY_PAID with cumulative amount")
    void payBill_partialPayment_success() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        // Bill total 355.04, already paid 100.00, outstanding 255.04.
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "PARTIALLY_PAID",
                        BILL_TOTAL, new BigDecimal("100.00"), new BigDecimal("255.04")));
        when(paymentReferenceGenerator.generate())
                .thenReturn("PAY-PART123");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(PaymentTransaction.class)))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY,
                validRequest("100.00"));

        verify(walletService).debit(USER_ID, new BigDecimal("100.00"));
        // Cumulative amount paid = 100 (previous) + 100 (now) = 200.00.
        verify(billServiceClient).notifyPaymentStatus(
                BILL_ID, "PARTIALLY_PAID", new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Pay: bill payment fails when the auth user is inactive")
    void payBill_inactiveUser_throws() {

        doThrow(new InactiveUserException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(InactiveUserException.class);

        // Nothing is reached: no bill fetch, no org check, no debit.
        verify(billServiceClient, never())
                .getConsumerBill(any(), any());
        verify(organizationServiceClient, never())
                .requireOrganizationAccess(any(), any(), any());
        verify(walletService, never()).debit(any(), any());
        verify(paymentRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Pay: bill payment fails when the auth user is not found")
    void payBill_unknownUser_throws() {

        doThrow(new UserNotFoundException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: insufficient wallet balance raises INSUFFICIENT_WALLET_BALANCE")
    void payBill_insufficientBalance_throws() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));
        doThrow(new InsufficientWalletBalanceException(
                "Insufficient wallet balance. Available: 10.00 INR"))
                .when(walletService).debit(USER_ID, BILL_TOTAL);

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(InsufficientWalletBalanceException.class)
                .hasMessageContaining("Insufficient wallet balance");

        verify(paymentRepository, never()).save(any(PaymentTransaction.class));
        verify(billServiceClient, never())
                .notifyPaymentStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Pay: already-paid bill is rejected without debiting")
    void payBill_alreadyPaidBill_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("PAID", "PAID",
                        BILL_TOTAL, BILL_TOTAL, BigDecimal.ZERO));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already fully paid");

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: cancelled bill is rejected")
    void payBill_cancelledBill_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("CANCELLED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED bills cannot be paid");

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: a bill whose payment status is not UNPAID/PARTIALLY_PAID is rejected")
    void payBill_invalidPaymentStatus_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "FAILED",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("UNPAID or PARTIALLY_PAID");

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: nonexistent bill propagates not-found")
    void payBill_nonexistentBill_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException("Bill", "id", BILL_ID));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: unauthorized bill access is rejected")
    void payBill_unauthorizedBill_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenThrow(new ForbiddenOperationException(
                        "You are not allowed to access this bill"));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: forbidden organization access is rejected")
    void payBill_forbiddenOrganization_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));
        doThrow(new ForbiddenOperationException(
                "You are not an active member of this organization"))
                .when(organizationServiceClient)
                .requireOrganizationAccess(ORG_ID, USER_ID, "CONSUMER");

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: amount above the outstanding amount is rejected")
    void payBill_amountAboveOutstanding_rejected() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY,
                validRequest("500.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds the outstanding amount");

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: duplicate idempotency key with same payload returns the original")
    void payBill_duplicateIdempotencyKey_returnsOriginal() {

        PaymentTransaction existing = buildPayment();

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));
        when(paymentMapper.toResponse(existing))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        PaymentResponse response = paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest());

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);

        verify(walletService, never()).debit(any(), any());
        verify(billServiceClient, never()).notifyPaymentStatus(any(), any(), any());
        verify(paymentRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Pay: same idempotency key with different payload is a conflict")
    void payBill_idempotencyKeyConflict_throws() {

        PaymentTransaction existing = buildPayment();
        existing.setAmount(new BigDecimal("500.00"));

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining(IDEMPOTENCY_KEY);

        verify(walletService, never()).debit(any(), any());
    }

    @Test
    @DisplayName("Pay: blank idempotency key is rejected")
    void payBill_blankIdempotencyKey_rejected() {

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, "   ", validRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Idempotency-Key");

        verify(paymentRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Pay: Bill Service notification failure propagates (no distributed transaction)")
    void payBill_upstreamFailure_propagates() {

        when(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));
        when(paymentReferenceGenerator.generate())
                .thenReturn("PAY-ABC123");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new UpstreamServiceException("Bill Service is down"))
                .when(billServiceClient)
                .notifyPaymentStatus(eq(BILL_ID), eq("PAID"), eq(BILL_TOTAL));

        assertThatThrownBy(() -> paymentService.payBillFromWallet(
                USER_ID, "CONSUMER", BILL_ID, IDEMPOTENCY_KEY, validRequest()))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("Bill Service is down");

        // The wallet was debited but the exception rolls the transaction
        // back — the payment is not recorded.
        verify(walletService).debit(USER_ID, BILL_TOTAL);
    }

    // ==================================================================
    // Read operations
    // ==================================================================

    @Test
    @DisplayName("Get by id: owner can read own payment")
    void getPaymentById_owned_returnsPayment() {

        PaymentTransaction payment = buildPayment();
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        PaymentResponse response = paymentService.getPaymentById(
                USER_ID, "CONSUMER", PAYMENT_ID);

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);
    }

    @Test
    @DisplayName("Get by id: another user's payment is forbidden")
    void getPaymentById_notOwned_throwsForbidden() {

        PaymentTransaction payment = buildPayment();
        payment.setUserId(OTHER_USER_ID);

        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(payment));
        doThrow(new ForbiddenOperationException(
                "You are not allowed to access this payment"))
                .when(accessHelper)
                .requirePaymentAccess(payment, USER_ID, "CONSUMER");

        assertThatThrownBy(() -> paymentService.getPaymentById(
                USER_ID, "CONSUMER", PAYMENT_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Get by id: missing payment returns not-found")
    void getPaymentById_missing_throwsNotFound() {

        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(
                USER_ID, "CONSUMER", PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("Get by reference: owner can read own payment")
    void getPaymentByReference_owned_returnsPayment() {

        PaymentTransaction payment = buildPayment();
        when(paymentRepository.findByPaymentReference("PAY-ABC123"))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        PaymentResponse response = paymentService.getPaymentByReference(
                USER_ID, "CONSUMER", "PAY-ABC123");

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);
    }

    @Test
    @DisplayName("List bill payments: owner can list")
    void getPaymentsForBill_owner_returnsList() {

        when(billServiceClient.getConsumerBill(BILL_ID, USER_ID))
                .thenReturn(payableBill("GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));
        when(paymentRepository.findByBillIdOrderByCreatedAtDesc(BILL_ID))
                .thenReturn(List.of(buildPayment()));
        when(paymentMapper.toResponse(any(PaymentTransaction.class)))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        assertThat(paymentService.getPaymentsForBill(
                USER_ID, "CONSUMER", BILL_ID)).hasSize(1);
    }

    @Test
    @DisplayName("List my payments: consumer gets own page")
    void getMyPayments_consumer_ownPage() {

        PaymentTransaction payment = buildPayment();
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(
                eq(USER_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));
        when(paymentMapper.toResponse(payment))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        Page<PaymentResponse> page = paymentService.getMyPayments(
                USER_ID, "CONSUMER", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(PAYMENT_ID);
    }

    @Test
    @DisplayName("List my payments: admin sees all payments")
    void getMyPayments_admin_allPage() {

        when(accessHelper.isSystemAdmin("ADMIN")).thenReturn(true);

        PaymentTransaction payment = buildPayment();
        when(paymentRepository.findAllByOrderByCreatedAtDesc(
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));
        when(paymentMapper.toResponse(payment))
                .thenReturn(PaymentResponse.builder().id(PAYMENT_ID).build());

        Page<PaymentResponse> page = paymentService.getMyPayments(
                USER_ID, "ADMIN", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private PayBillRequest validRequest() {
        return validRequest(BILL_TOTAL.toPlainString());
    }

    private PayBillRequest validRequest(String amount) {
        return PayBillRequest.builder()
                .amount(new BigDecimal(amount))
                .currency(Currency.INR)
                .organizationId(ORG_ID)
                .build();
    }

    private BillSnapshot payableBill(
            String billStatus, String paymentStatus,
            BigDecimal total, BigDecimal amountPaid, BigDecimal outstanding) {

        return new BillSnapshot(
                BILL_ID, USER_ID, billStatus, paymentStatus,
                total, amountPaid, outstanding);
    }

    private PaymentTransaction buildPayment() {

        return PaymentTransaction.builder()
                .id(PAYMENT_ID)
                .paymentReference("PAY-ABC123")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .transactionType(TransactionType.BILL_PAYMENT)
                .billId(BILL_ID)
                .organizationId(ORG_ID)
                .userId(USER_ID)
                .amount(BILL_TOTAL)
                .currency(Currency.INR)
                .paymentMethod(PaymentMethod.WALLET)
                .status(PaymentStatus.SUCCESS)
                .build();
    }
}

package com.voltaras.paymentservice.service.impl;

import com.voltaras.paymentservice.client.BillServiceClient;
import com.voltaras.paymentservice.client.BillSnapshot;
import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.dto.request.PayBillRequest;
import com.voltaras.paymentservice.dto.response.PaymentResponse;
import com.voltaras.paymentservice.entity.PaymentTransaction;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.enums.TransactionType;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.exception.BusinessRuleException;
import com.voltaras.paymentservice.exception.IdempotencyConflictException;
import com.voltaras.paymentservice.exception.ResourceNotFoundException;
import com.voltaras.paymentservice.mapper.PaymentMapper;
import com.voltaras.paymentservice.repository.PaymentTransactionRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.PaymentService;
import com.voltaras.paymentservice.service.UserVerificationService;
import com.voltaras.paymentservice.service.WalletService;
import com.voltaras.paymentservice.util.IdempotencyKeyValidator;
import com.voltaras.paymentservice.util.MoneyUtils;
import com.voltaras.paymentservice.util.PaymentReferenceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link PaymentService}.
 *
 * <p>Business rules enforced here:</p>
 *
 * <ul>
 *     <li>the authenticated user must exist and be active in the Auth
 *         Service (verified through the internal user API, never by
 *         reading the auth database)</li>
 *     <li>the bill must exist, belong to the requester (or be accessible
 *         to a system ADMIN) and be UNPAID or PARTIALLY_PAID</li>
 *     <li>the requester must be an active member of the organization</li>
 *     <li>the wallet balance must cover the payment; otherwise
 *         INSUFFICIENT_WALLET_BALANCE is raised</li>
 *     <li>the payment amount must be positive and at most the outstanding
 *         amount of the bill</li>
 *     <li>idempotency keys never create duplicate payments and reject
 *         conflicting payloads</li>
 *     <li>the wallet debit and the Bill Service notification happen inside
 *         one transaction; there are no distributed transactions</li>
 *     <li>no sensitive payment data is ever accepted or stored</li>
 * </ul>
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String BILL_STATUS_PAID = "PAID";
    private static final String BILL_STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_PARTIALLY_PAID =
            "PARTIALLY_PAID";
    private static final String PAYMENT_STATUS_UNPAID = "UNPAID";

    private final PaymentTransactionRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentAccessHelper accessHelper;
    private final WalletService walletService;
    private final BillServiceClient billServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final UserVerificationService userVerificationService;
    private final PaymentReferenceGenerator paymentReferenceGenerator;

    public PaymentServiceImpl(
            PaymentTransactionRepository paymentRepository,
            PaymentMapper paymentMapper,
            PaymentAccessHelper accessHelper,
            WalletService walletService,
            BillServiceClient billServiceClient,
            OrganizationServiceClient organizationServiceClient,
            UserVerificationService userVerificationService,
            PaymentReferenceGenerator paymentReferenceGenerator) {

        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.accessHelper = accessHelper;
        this.walletService = walletService;
        this.billServiceClient = billServiceClient;
        this.organizationServiceClient = organizationServiceClient;
        this.userVerificationService = userVerificationService;
        this.paymentReferenceGenerator = paymentReferenceGenerator;
    }

    // ==================================================================
    // Pay bill from wallet
    // ==================================================================

    @Override
    @Transactional
    public PaymentResponse payBillFromWallet(
            Long authUserId, String systemRole, Long billId,
            String idempotencyKey, PayBillRequest request) {

        accessHelper.requireAuthenticatedUser(authUserId);
        IdempotencyKeyValidator.requireValid(idempotencyKey);

        // Cheap local validation first: a non-positive amount fails with
        // 400 without depending on any remote service being reachable.
        BigDecimal amount = MoneyUtils.scale(request.getAmount());

        if (amount.signum() <= 0) {
            throw new BusinessRuleException(
                    "Payment amount must be greater than zero");
        }

        // The user must exist and be active in the Auth Service; the
        // user ID and role are cross-checked against the gateway headers.
        userVerificationService.verifyActiveUser(authUserId, systemRole);

        // Idempotent replay: an existing key with the same payload returns
        // the original payment without debiting the wallet again.
        Optional<PaymentTransaction> existing = paymentRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {

            PaymentTransaction payment = existing.get();

            if (matchesRequest(payment, billId, request)) {

                log.info("Idempotent replay of key {} returned payment {}",
                        idempotencyKey, payment.getId());

                return paymentMapper.toResponse(payment);
            }

            throw new IdempotencyConflictException(idempotencyKey);
        }

        // Bill must exist and be payable, and the requester must be
        // authorized to access it.
        BillSnapshot bill = fetchAuthorizedBill(authUserId, systemRole, billId);

        requirePayableBill(bill);

        // Organization access is validated through the Organization Service.
        organizationServiceClient.requireOrganizationAccess(
                request.getOrganizationId(), authUserId, systemRole);

        BigDecimal outstanding = outstandingAmount(bill);

        if (amount.compareTo(outstanding) > 0) {

            throw new BusinessRuleException(
                    "Payment amount exceeds the outstanding amount "
                            + outstanding.toPlainString()
                            + " " + request.getCurrency());
        }

        // Debit the wallet; raises INSUFFICIENT_WALLET_BALANCE when the
        // balance is too low. The Bill Service is notified afterwards, all
        // inside this transaction.
        walletService.debit(authUserId, amount);

        BigDecimal cumulativeAmountPaid = cumulativeAmountPaid(bill)
                .add(amount);

        String targetStatus = cumulativeAmountPaid
                .compareTo(bill.totalAmount()) >= 0
                ? PAYMENT_STATUS_PAID
                : PAYMENT_STATUS_PARTIALLY_PAID;

        PaymentTransaction payment = PaymentTransaction.builder()
                .paymentReference(paymentReferenceGenerator.generate())
                .idempotencyKey(idempotencyKey)
                .transactionType(TransactionType.BILL_PAYMENT)
                .billId(billId)
                .organizationId(request.getOrganizationId())
                .userId(authUserId)
                .amount(amount)
                .currency(request.getCurrency())
                .paymentMethod(PaymentMethod.WALLET)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Notify the Bill Service only after the wallet debit. A
        // notification failure aborts the transaction (the debit is rolled
        // back) — there are deliberately no distributed transactions.
        billServiceClient.notifyPaymentStatus(
                billId, targetStatus, cumulativeAmountPaid);

        log.info("Bill {} paid {} INR from wallet of user {} -> {}",
                billId, amount, authUserId, targetStatus);

        return paymentMapper.toResponse(payment);
    }

    // ==================================================================
    // Read operations
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(
            Long authUserId, String systemRole, Long paymentId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        PaymentTransaction payment = requirePayment(paymentId);

        accessHelper.requirePaymentAccess(payment, authUserId, systemRole);

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(
            Long authUserId, String systemRole, String paymentReference) {

        accessHelper.requireAuthenticatedUser(authUserId);

        if (paymentReference == null || paymentReference.isBlank()) {
            throw new BadRequestException(
                    "Payment reference is required");
        }

        PaymentTransaction payment = paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "paymentReference", paymentReference));

        accessHelper.requirePaymentAccess(payment, authUserId, systemRole);

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForBill(
            Long authUserId, String systemRole, Long billId) {

        accessHelper.requireAuthenticatedUser(authUserId);

        // Validates that the bill exists and that the caller may access it.
        fetchAuthorizedBill(authUserId, systemRole, billId);

        List<PaymentTransaction> payments = paymentRepository
                .findByBillIdOrderByCreatedAtDesc(billId);

        return payments.stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(
            Long authUserId, String systemRole,
            Long organizationId, Pageable pageable) {

        accessHelper.requireAuthenticatedUser(authUserId);

        if (accessHelper.isSystemAdmin(systemRole)) {

            if (organizationId != null) {

                return paymentRepository
                        .findByOrganizationIdOrderByCreatedAtDesc(
                                organizationId, pageable)
                        .map(paymentMapper::toResponse);
            }

            return paymentRepository
                    .findAllByOrderByCreatedAtDesc(pageable)
                    .map(paymentMapper::toResponse);
        }

        return paymentRepository
                .findByUserIdOrderByCreatedAtDesc(authUserId, pageable)
                .map(paymentMapper::toResponse);
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    private PaymentTransaction requirePayment(Long paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "id", paymentId));
    }

    private BillSnapshot fetchAuthorizedBill(
            Long authUserId, String systemRole, Long billId) {

        if (accessHelper.isSystemAdmin(systemRole)) {
            return billServiceClient.getBillAsAdmin(billId, systemRole);
        }

        return billServiceClient.getConsumerBill(billId, authUserId);
    }

    /**
     * A bill is payable from the wallet only when its payment status is
     * UNPAID or PARTIALLY_PAID and it is not PAID/CANCELLED.
     */
    private void requirePayableBill(BillSnapshot bill) {

        if (PAYMENT_STATUS_PAID.equals(bill.paymentStatus())
                || BILL_STATUS_PAID.equals(bill.billStatus())) {

            throw new BusinessRuleException(
                    "Bill is already fully paid");
        }

        if (BILL_STATUS_CANCELLED.equals(bill.billStatus())) {

            throw new BusinessRuleException(
                    "CANCELLED bills cannot be paid");
        }

        if (!PAYMENT_STATUS_UNPAID.equals(bill.paymentStatus())
                && !PAYMENT_STATUS_PARTIALLY_PAID.equals(
                        bill.paymentStatus())) {

            throw new BusinessRuleException(
                    "Bill must be UNPAID or PARTIALLY_PAID to be paid "
                            + "from the wallet");
        }
    }

    private BigDecimal outstandingAmount(BillSnapshot bill) {

        if (bill.outstandingAmount() != null) {
            return MoneyUtils.scale(bill.outstandingAmount());
        }

        return MoneyUtils.scale(bill.totalAmount())
                .subtract(cumulativeAmountPaid(bill));
    }

    private BigDecimal cumulativeAmountPaid(BillSnapshot bill) {

        return bill.amountPaid() != null
                ? MoneyUtils.scale(bill.amountPaid())
                : BigDecimal.ZERO.setScale(MoneyUtils.SCALE);
    }

    /**
     * Compares an existing idempotent payment against the current request
     * so a reused key with a different payload is rejected.
     */
    private boolean matchesRequest(
            PaymentTransaction payment, Long billId, PayBillRequest request) {

        return payment.getBillId().equals(billId)
                && payment.getOrganizationId()
                .equals(request.getOrganizationId())
                && payment.getAmount()
                .compareTo(MoneyUtils.scale(request.getAmount())) == 0
                && payment.getCurrency() == request.getCurrency();
    }
}

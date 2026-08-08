package com.voltaras.paymentservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.dto.request.CreateRechargeOrderRequest;
import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;
import com.voltaras.paymentservice.entity.RechargeTransaction;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.IdempotencyConflictException;
import com.voltaras.paymentservice.exception.InvalidStateException;
import com.voltaras.paymentservice.mapper.RechargeTransactionMapper;
import com.voltaras.paymentservice.provider.RazorpayCreateOrderRequest;
import com.voltaras.paymentservice.provider.RazorpayGatewayClient;
import com.voltaras.paymentservice.provider.RazorpayOrder;
import com.voltaras.paymentservice.repository.RechargeTransactionRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.RechargeService;
import com.voltaras.paymentservice.service.UserVerificationService;
import com.voltaras.paymentservice.service.WalletService;
import com.voltaras.paymentservice.util.IdempotencyKeyValidator;
import com.voltaras.paymentservice.util.MoneyUtils;
import com.voltaras.paymentservice.util.PaymentReferenceGenerator;
import com.voltaras.paymentservice.util.PaymentStatusTransitions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link RechargeService}.
 *
 * <p>
 * A recharge order is created at the Razorpay gateway (amount in paise) and
 * stored as {@link PaymentStatus#CREATED}. The wallet is credited only when
 * the gateway confirms the payment through a signature-protected webhook.
 * Webhook callbacks are idempotent: replaying the same event never credits
 * the wallet twice.
 * </p>
 */
@Service
@Slf4j
public class RechargeServiceImpl implements RechargeService {

    private static final String CURRENCY_INR = "INR";
    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    private static final String EVENT_PAYMENT_FAILED = "payment.failed";
    private static final String EVENT_PAYMENT_AUTHORIZED = "payment.authorized";
    private static final String EVENT_ORDER_PAID = "order.paid";

    private static final List<PaymentStatus> TERMINAL_STATUSES =
            List.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED,
                    PaymentStatus.CANCELLED, PaymentStatus.REFUNDED);

    private final RechargeTransactionRepository rechargeRepository;
    private final RechargeTransactionMapper rechargeMapper;
    private final PaymentAccessHelper accessHelper;
    private final OrganizationServiceClient organizationServiceClient;
    private final RazorpayGatewayClient razorpayGatewayClient;
    private final WalletService walletService;
    private final UserVerificationService userVerificationService;
    private final PaymentReferenceGenerator referenceGenerator;
    private final ObjectMapper objectMapper;
    private final String razorpayKeyId;
    private final String razorpayWebhookSecret;

    public RechargeServiceImpl(
            RechargeTransactionRepository rechargeRepository,
            RechargeTransactionMapper rechargeMapper,
            PaymentAccessHelper accessHelper,
            OrganizationServiceClient organizationServiceClient,
            RazorpayGatewayClient razorpayGatewayClient,
            WalletService walletService,
            UserVerificationService userVerificationService,
            PaymentReferenceGenerator referenceGenerator,
            ObjectMapper objectMapper,
            @Value("${app.razorpay.key-id}") String razorpayKeyId,
            @Value("${app.razorpay.webhook-secret}") String razorpayWebhookSecret) {

        this.rechargeRepository = rechargeRepository;
        this.rechargeMapper = rechargeMapper;
        this.accessHelper = accessHelper;
        this.organizationServiceClient = organizationServiceClient;
        this.razorpayGatewayClient = razorpayGatewayClient;
        this.walletService = walletService;
        this.userVerificationService = userVerificationService;
        this.referenceGenerator = referenceGenerator;
        this.objectMapper = objectMapper;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    // ==================================================================
    // Create recharge order
    // ==================================================================

    @Override
    @Transactional
    public RechargeOrderResponse createRechargeOrder(
            Long authUserId, String systemRole, String idempotencyKey,
            CreateRechargeOrderRequest request) {

        accessHelper.requireAuthenticatedUser(authUserId);
        IdempotencyKeyValidator.requireValid(idempotencyKey);

        // Cheap local validation first: malformed requests fail with 400
        // without depending on the Auth Service being reachable.
        requireRechargeMethod(request);

        BigDecimal amount = MoneyUtils.scale(request.getAmount());

        if (amount.signum() <= 0) {
            throw new BadRequestException(
                    "Recharge amount must be greater than zero");
        }

        // The user must exist and be active in the Auth Service; the
        // user ID and role are cross-checked against the gateway headers.
        userVerificationService.verifyActiveUser(authUserId, systemRole);

        // Idempotent replay: same key with the same payload returns the
        // original order without calling the gateway again.
        Optional<RechargeTransaction> existing =
                rechargeRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {

            RechargeTransaction recharge = existing.get();

            if (matchesRequest(recharge, request)) {

                log.info("Idempotent replay of recharge key {} returned "
                        + "order {}", idempotencyKey, recharge.getOrderId());

                return toOrderResponse(recharge);
            }

            throw new IdempotencyConflictException(idempotencyKey);
        }

        // Active membership in the organization is required.
        organizationServiceClient.requireOrganizationAccess(
                request.getOrganizationId(), authUserId, systemRole);

        String rechargeReference = referenceGenerator.generate("RCH");

        // Amounts are sent to the gateway in paise.
        RazorpayOrder razorpayOrder = razorpayGatewayClient.createOrder(
                new RazorpayCreateOrderRequest(
                        MoneyUtils.toPaise(amount),
                        CURRENCY_INR,
                        rechargeReference,
                        notes(authUserId, request)));

        RechargeTransaction recharge = RechargeTransaction.builder()
                .rechargeReference(rechargeReference)
                .orderId(razorpayOrder.id())
                .idempotencyKey(idempotencyKey)
                .userId(authUserId)
                .organizationId(request.getOrganizationId())
                .amount(amount)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .provider(PaymentProvider.RAZORPAY)
                .status(PaymentStatus.CREATED)
                .build();

        RechargeTransaction saved = rechargeRepository.save(recharge);

        log.info("Recharge order {} ({} INR, {}) created by user {}",
                saved.getOrderId(), saved.getAmount(),
                saved.getPaymentMethod(), authUserId);

        return toOrderResponse(saved);
    }

    // ==================================================================
    // Razorpay webhook
    // ==================================================================

    @Override
    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {

        if (!razorpayGatewayClient.verifyWebhookSignature(
                payload, signature, razorpayWebhookSecret)) {

            throw new ForbiddenOperationException(
                    "Invalid Razorpay webhook signature");
        }

        JsonNode root;
        String event;

        try {

            root = objectMapper.readTree(payload);
            event = root.path("event").asText();

        } catch (Exception ex) {

            throw new BadRequestException(
                    "Malformed Razorpay webhook payload");
        }

        switch (event) {

            case EVENT_PAYMENT_CAPTURED,
                    EVENT_PAYMENT_FAILED,
                    EVENT_PAYMENT_AUTHORIZED -> {

                JsonNode entity = root
                        .path("payload").path("payment").path("entity");

                handlePaymentEvent(
                        event,
                        entity.path("order_id").asText(),
                        entity.path("id").asText(),
                        entity.path("amount").asLong());
            }

            case EVENT_ORDER_PAID -> {

                JsonNode entity = root
                        .path("payload").path("order").path("entity");

                handlePaymentEvent(
                        event,
                        entity.path("id").asText(),
                        null,
                        entity.path("amount").asLong());
            }

            default -> log.info("Ignoring Razorpay webhook event {}",
                    event);
        }
    }

    /**
     * Applies a single webhook event to the matching recharge order. The
     * wallet is credited only for captured/paid events and only once.
     */
    private void handlePaymentEvent(
            String event, String orderId, String paymentId, long amountPaise) {

        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException(
                    "Webhook payload does not contain an order ID");
        }

        // The row is pessimistically locked so concurrent deliveries of the
        // same event are serialized and the wallet can never be credited twice.
        RechargeTransaction recharge =
                rechargeRepository.findByOrderIdForUpdate(orderId).orElse(null);

        // Unknown orders are ignored with a 200 so the gateway does not
        // retry events this service is not responsible for.
        if (recharge == null) {

            log.warn("Webhook {} received for unknown order {}; ignored",
                    event, orderId);

            return;
        }

        // Terminal states ignore all further events. This makes callbacks
        // idempotent even when they are delivered concurrently or out of
        // order (for example a late payment.failed for an already captured
        // order is a no-op instead of an error).
        if (TERMINAL_STATUSES.contains(recharge.getStatus())) {

            log.info("Ignoring webhook {} for order {} (terminal {})",
                    event, orderId, recharge.getStatus());

            return;
        }

        switch (event) {

            case EVENT_PAYMENT_CAPTURED, EVENT_ORDER_PAID ->
                    completeSuccess(recharge, paymentId, amountPaise);

            case EVENT_PAYMENT_FAILED ->
                    completeFailed(recharge, paymentId);

            case EVENT_PAYMENT_AUTHORIZED ->
                    markPending(recharge);

            default -> log.info("Ignoring webhook event {} for order {}",
                    event, orderId);
        }
    }

    private void completeSuccess(
            RechargeTransaction recharge, String paymentId, long amountPaise) {

        // The caller only reaches here for non-terminal statuses (CREATED or
        // PENDING), so this transition is always valid.
        BigDecimal gatewayAmount = MoneyUtils.fromPaise(amountPaise);

        if (gatewayAmount.compareTo(recharge.getAmount()) != 0) {

            log.warn("Webhook amount {} does not match order {} amount {}; "
                    + "marking FAILED", gatewayAmount, recharge.getOrderId(),
                    recharge.getAmount());

            transition(recharge.getStatus(), PaymentStatus.FAILED);
            recharge.setStatus(PaymentStatus.FAILED);
            recharge.setFailureCode("RAZORPAY_AMOUNT_MISMATCH");
            recharge.setFailureReason(
                    "Webhook amount does not match the order amount");
            recharge.setProviderTransactionId(paymentId);
            rechargeRepository.save(recharge);

            return;
        }

        transition(recharge.getStatus(), PaymentStatus.SUCCESS);

        recharge.setStatus(PaymentStatus.SUCCESS);
        recharge.setProviderTransactionId(paymentId);
        recharge.setFailureCode(null);
        recharge.setFailureReason(null);
        recharge.setPaidAt(LocalDateTime.now());

        rechargeRepository.save(recharge);

        walletService.credit(recharge.getUserId(), recharge.getAmount());

        log.info("Recharge order {} completed; wallet of user {} credited {}",
                recharge.getOrderId(), recharge.getUserId(),
                recharge.getAmount());
    }

    private void completeFailed(
            RechargeTransaction recharge, String paymentId) {

        // The caller only reaches here for non-terminal statuses (CREATED or
        // PENDING), so this transition is always valid.
        transition(recharge.getStatus(), PaymentStatus.FAILED);

        recharge.setStatus(PaymentStatus.FAILED);
        recharge.setProviderTransactionId(paymentId);
        recharge.setFailureCode("RAZORPAY_PAYMENT_FAILED");
        recharge.setFailureReason(
                "Razorpay reported the payment as failed");
        recharge.setPaidAt(null);

        rechargeRepository.save(recharge);

        log.info("Recharge order {} failed", recharge.getOrderId());
    }

    private void markPending(RechargeTransaction recharge) {

        // Already authorized (PENDING) or reached via a non-terminal state
        // is a no-op.
        if (recharge.getStatus() == PaymentStatus.PENDING) {
            return;
        }

        transition(recharge.getStatus(), PaymentStatus.PENDING);

        recharge.setStatus(PaymentStatus.PENDING);
        rechargeRepository.save(recharge);

        log.info("Recharge order {} authorized, awaiting capture",
                recharge.getOrderId());
    }

    // ==================================================================
    // Read operations
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<RechargeTransactionResponse> getMyRecharges(
            Long authUserId, String systemRole) {

        accessHelper.requireAuthenticatedUser(authUserId);

        // The user must exist and be active in the Auth Service.
        userVerificationService.verifyActiveUser(authUserId, systemRole);

        return rechargeRepository
                .findByUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(rechargeMapper::toTransactionResponse)
                .toList();
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    private RechargeOrderResponse toOrderResponse(RechargeTransaction recharge) {

        RechargeOrderResponse response =
                rechargeMapper.toOrderResponse(recharge);

        response.setRazorpayKeyId(razorpayKeyId);

        return response;
    }

    private void requireRechargeMethod(CreateRechargeOrderRequest request) {

        if (request.getPaymentMethod() != PaymentMethod.UPI
                && request.getPaymentMethod() != PaymentMethod.CARD) {

            throw new BadRequestException(
                    "Recharge payment method must be UPI or CARD");
        }
    }

    private boolean matchesRequest(
            RechargeTransaction recharge, CreateRechargeOrderRequest request) {

        return recharge.getOrganizationId()
                .equals(request.getOrganizationId())
                && recharge.getAmount()
                .compareTo(MoneyUtils.scale(request.getAmount())) == 0
                && recharge.getCurrency() == request.getCurrency()
                && recharge.getPaymentMethod() == request.getPaymentMethod();
    }

    private Map<String, String> notes(
            Long authUserId, CreateRechargeOrderRequest request) {

        Map<String, String> notes = new HashMap<>();

        notes.put("userId", String.valueOf(authUserId));
        notes.put("organizationId",
                String.valueOf(request.getOrganizationId()));

        if (request.getNote() != null && !request.getNote().isBlank()) {
            notes.put("note", request.getNote());
        }

        return notes;
    }

    private void transition(PaymentStatus from, PaymentStatus to) {

        if (!PaymentStatusTransitions.canTransition(from, to)) {

            throw new InvalidStateException(
                    "Invalid recharge status transition from " + from
                            + " to " + to);
        }
    }
}

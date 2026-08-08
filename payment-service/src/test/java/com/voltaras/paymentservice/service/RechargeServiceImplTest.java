package com.voltaras.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.dto.request.CreateRechargeOrderRequest;
import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;
import com.voltaras.paymentservice.entity.RechargeTransaction;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.IdempotencyConflictException;
import com.voltaras.paymentservice.exception.InactiveUserException;
import com.voltaras.paymentservice.exception.PaymentProviderException;
import com.voltaras.paymentservice.exception.UserNotFoundException;
import com.voltaras.paymentservice.mapper.RechargeTransactionMapper;
import com.voltaras.paymentservice.provider.RazorpayCreateOrderRequest;
import com.voltaras.paymentservice.provider.RazorpayGatewayClient;
import com.voltaras.paymentservice.provider.RazorpayOrder;
import com.voltaras.paymentservice.repository.RechargeTransactionRepository;
import com.voltaras.paymentservice.security.PaymentAccessHelper;
import com.voltaras.paymentservice.service.impl.RechargeServiceImpl;
import com.voltaras.paymentservice.util.PaymentReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RechargeServiceImpl}: order creation (UPI/CARD),
 * idempotency, Razorpay webhook processing and wallet crediting.
 */
@ExtendWith(MockitoExtension.class)
class RechargeServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long ORG_ID = 6L;
    private static final String IDEMPOTENCY_KEY = "recharge-500-upi-1";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";
    private static final String KEY_ID = "rzp_test_0000000000000000";

    @Mock private RechargeTransactionRepository rechargeRepository;
    @Mock private RechargeTransactionMapper rechargeMapper;
    @Mock private PaymentAccessHelper accessHelper;
    @Mock private OrganizationServiceClient organizationServiceClient;
    @Mock private RazorpayGatewayClient razorpayGatewayClient;
    @Mock private WalletService walletService;
    @Mock private UserVerificationService userVerificationService;
    @Mock private PaymentReferenceGenerator referenceGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RechargeServiceImpl rechargeService;

    @BeforeEach
    void setUp() {
        rechargeService = new RechargeServiceImpl(
                rechargeRepository,
                rechargeMapper,
                accessHelper,
                organizationServiceClient,
                razorpayGatewayClient,
                walletService,
                userVerificationService,
                referenceGenerator,
                objectMapper,
                KEY_ID,
                WEBHOOK_SECRET);
    }

    // ==================================================================
    // Create recharge order
    // ==================================================================

    @Test
    @DisplayName("Create: UPI order is created at the gateway in paise")
    void createOrder_upi_success() {

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(referenceGenerator.generate("RCH"))
                .thenReturn("RCH-ABC123");
        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_test_upi", 50000, "INR", "created", "RCH-ABC123"));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(rechargeMapper.toOrderResponse(any(RechargeTransaction.class)))
                .thenReturn(RechargeOrderResponse.builder()
                        .id(1L)
                        .orderId("order_test_upi")
                        .razorpayKeyId(KEY_ID)
                        .build());

        RechargeOrderResponse response = rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI));

        assertThat(response.getOrderId()).isEqualTo("order_test_upi");
        assertThat(response.getRazorpayKeyId()).isEqualTo(KEY_ID);

        // Amounts are sent to the gateway in paise.
        ArgumentCaptor<RazorpayCreateOrderRequest> captor =
                ArgumentCaptor.forClass(RazorpayCreateOrderRequest.class);
        verify(razorpayGatewayClient).createOrder(captor.capture());

        assertThat(captor.getValue().amount()).isEqualTo(50000L);
        assertThat(captor.getValue().currency()).isEqualTo("INR");
        assertThat(captor.getValue().receipt()).isEqualTo("RCH-ABC123");
        assertThat(captor.getValue().notes())
                .containsEntry("userId", "100")
                .containsEntry("organizationId", "6");

        verify(organizationServiceClient)
                .requireOrganizationAccess(ORG_ID, USER_ID, "CONSUMER");

        // The active auth user was verified with the Auth Service first.
        verify(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        ArgumentCaptor<RechargeTransaction> saved =
                ArgumentCaptor.forClass(RechargeTransaction.class);
        verify(rechargeRepository).save(saved.capture());

        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(saved.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(saved.getValue().getProvider()).isEqualTo(PaymentProvider.RAZORPAY);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Create: CARD order is created at the gateway")
    void createOrder_card_success() {

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(referenceGenerator.generate("RCH"))
                .thenReturn("RCH-CARD123");
        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_test_card", 25000, "INR", "created", "RCH-CARD123"));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(rechargeMapper.toOrderResponse(any(RechargeTransaction.class)))
                .thenReturn(RechargeOrderResponse.builder()
                        .id(1L)
                        .orderId("order_test_card")
                        .build());

        RechargeOrderResponse response = rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.CARD, "250.00"));

        assertThat(response.getOrderId()).isEqualTo("order_test_card");

        ArgumentCaptor<RazorpayCreateOrderRequest> captor =
                ArgumentCaptor.forClass(RazorpayCreateOrderRequest.class);
        verify(razorpayGatewayClient).createOrder(captor.capture());

        assertThat(captor.getValue().amount()).isEqualTo(25000L);

        ArgumentCaptor<RechargeTransaction> saved =
                ArgumentCaptor.forClass(RechargeTransaction.class);
        verify(rechargeRepository).save(saved.capture());

        assertThat(saved.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("Create: duplicate idempotency key with same payload returns the original order")
    void createOrder_duplicateKey_returnsOriginal() {

        RechargeTransaction existing = buildRecharge(PaymentStatus.CREATED);

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));
        when(rechargeMapper.toOrderResponse(existing))
                .thenReturn(RechargeOrderResponse.builder()
                        .id(1L)
                        .orderId("order_original")
                        .razorpayKeyId(KEY_ID)
                        .build());

        RechargeOrderResponse response = rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI));

        assertThat(response.getOrderId()).isEqualTo("order_original");

        verify(razorpayGatewayClient, never()).createOrder(any());
        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
        verify(organizationServiceClient, never())
                .requireOrganizationAccess(any(), any(), any());
    }

    @Test
    @DisplayName("Create: same key with different payload is a conflict")
    void createOrder_idempotencyConflict_throws() {

        RechargeTransaction existing = buildRecharge(PaymentStatus.CREATED);
        existing.setAmount(new BigDecimal("999.00"));

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI)))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining(IDEMPOTENCY_KEY);

        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
    }

    @Test
    @DisplayName("Create: forbidden organization access is rejected")
    void createOrder_forbiddenOrganization_throws() {

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        doThrow(new ForbiddenOperationException(
                "You are not an active member of this organization"))
                .when(organizationServiceClient)
                .requireOrganizationAccess(ORG_ID, USER_ID, "CONSUMER");

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
        verify(razorpayGatewayClient, never()).createOrder(any());
    }

    @Test
    @DisplayName("Create: inactive auth user cannot create a recharge order")
    void createOrder_inactiveUser_throws() {

        doThrow(new InactiveUserException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI)))
                .isInstanceOf(InactiveUserException.class);

        // Nothing else is reached: no organization check, no gateway
        // call, nothing is saved.
        verify(organizationServiceClient, never())
                .requireOrganizationAccess(any(), any(), any());
        verify(razorpayGatewayClient, never()).createOrder(any());
        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
    }

    @Test
    @DisplayName("Create: unknown auth user id returns USER_NOT_FOUND")
    void createOrder_unknownUser_throws() {

        doThrow(new UserNotFoundException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));

        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
        verify(razorpayGatewayClient, never()).createOrder(any());
    }

    @Test
    @DisplayName("Create: WALLET is not a valid recharge method")
    void createOrder_walletMethod_rejected() {

        CreateRechargeOrderRequest request =
                validRequest(PaymentMethod.WALLET);

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UPI or CARD");

        verify(razorpayGatewayClient, never()).createOrder(any());
    }

    @Test
    @DisplayName("Create: gateway failure propagates and nothing is saved")
    void createOrder_gatewayFailure_propagates() {

        when(rechargeRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(referenceGenerator.generate("RCH"))
                .thenReturn("RCH-ABC123");
        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenThrow(new PaymentProviderException(
                        "Razorpay gateway failed to create the order"));

        assertThatThrownBy(() -> rechargeService.createRechargeOrder(
                USER_ID, "CONSUMER", IDEMPOTENCY_KEY,
                validRequest(PaymentMethod.UPI)))
                .isInstanceOf(PaymentProviderException.class);

        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
    }

    // ==================================================================
    // Razorpay webhook
    // ==================================================================

    @Test
    @DisplayName("Webhook: captured payment credits the wallet once")
    void webhook_captured_creditsWallet() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.CREATED);

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rechargeService.handleRazorpayWebhook(
                capturedPayload("order_1", "pay_1", 50000),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(recharge.getProviderTransactionId()).isEqualTo("pay_1");
        assertThat(recharge.getPaidAt()).isNotNull();

        verify(walletService).credit(USER_ID, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Webhook: replayed captured event does not credit the wallet again")
    void webhook_replayedCapture_noDoubleCredit() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.SUCCESS);
        recharge.setProviderTransactionId("pay_1");

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));

        rechargeService.handleRazorpayWebhook(
                capturedPayload("order_1", "pay_1", 50000),
                "any-signature");

        verify(walletService, never()).credit(any(), any());
        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
    }

    @Test
    @DisplayName("Webhook: failed payment does not credit the wallet")
    void webhook_failed_noCredit() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.CREATED);

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rechargeService.handleRazorpayWebhook(
                failedPayload("order_1", "pay_1"),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(recharge.getFailureCode())
                .isEqualTo("RAZORPAY_PAYMENT_FAILED");
        assertThat(recharge.getPaidAt()).isNull();

        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("Webhook: out-of-order events for a SUCCESS order are ignored (no error)")
    void webhook_outOfOrderEvent_afterSuccess_ignored() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.SUCCESS);
        recharge.setProviderTransactionId("pay_1");

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));

        // A late payment.failed for an already-captured order is a no-op.
        rechargeService.handleRazorpayWebhook(
                failedPayload("order_1", "pay_late"),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("Webhook: order.paid event credits the wallet")
    void webhook_orderPaid_creditsWallet() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.PENDING);

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_2"))
                .thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rechargeService.handleRazorpayWebhook(
                orderPaidPayload("order_2", 50000),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(walletService).credit(USER_ID, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Webhook: amount mismatch marks the recharge FAILED without crediting")
    void webhook_amountMismatch_marksFailed() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.CREATED);

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Gateway reports 300.00 INR while the order was 500.00 INR.
        rechargeService.handleRazorpayWebhook(
                capturedPayload("order_1", "pay_1", 30000),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(recharge.getFailureCode())
                .isEqualTo("RAZORPAY_AMOUNT_MISMATCH");

        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("Webhook: authorized event moves the order to PENDING")
    void webhook_authorized_marksPending() {

        RechargeTransaction recharge = buildRecharge(PaymentStatus.CREATED);

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_1"))
                .thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(RechargeTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rechargeService.handleRazorpayWebhook(
                authorizedPayload("order_1", "pay_1"),
                "any-signature");

        assertThat(recharge.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("Webhook: unknown order is ignored (idempotent, no error)")
    void webhook_unknownOrder_ignored() {

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);
        when(rechargeRepository.findByOrderIdForUpdate("order_unknown"))
                .thenReturn(Optional.empty());

        rechargeService.handleRazorpayWebhook(
                capturedPayload("order_unknown", "pay_x", 50000),
                "any-signature");

        verify(rechargeRepository, never()).save(any(RechargeTransaction.class));
        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("Webhook: invalid signature is rejected")
    void webhook_invalidSignature_throwsForbidden() {

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> rechargeService.handleRazorpayWebhook(
                "{}", "bad-signature"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("signature");

        verify(rechargeRepository, never()).findByOrderIdForUpdate(any());
    }

    @Test
    @DisplayName("Webhook: malformed payload is rejected after signature passes")
    void webhook_malformedPayload_throwsBadRequest() {

        when(razorpayGatewayClient.verifyWebhookSignature(
                any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> rechargeService.handleRazorpayWebhook(
                "{not-json", "any-signature"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Malformed");
    }

    // ==================================================================
    // Read operations
    // ==================================================================

    @Test
    @DisplayName("Recharge history returns the user's recharges after auth verification")
    void getMyRecharges_returnsHistory() {

        when(rechargeRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(buildRecharge(PaymentStatus.SUCCESS)));
        when(rechargeMapper.toTransactionResponse(any(RechargeTransaction.class)))
                .thenReturn(RechargeTransactionResponse.builder()
                        .id(1L)
                        .status(PaymentStatus.SUCCESS)
                        .build());

        List<RechargeTransactionResponse> recharges =
                rechargeService.getMyRecharges(USER_ID, "CONSUMER");

        assertThat(recharges).hasSize(1);
        assertThat(recharges.get(0).getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);

        verify(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");
    }

    @Test
    @DisplayName("Recharge history is rejected for an inactive auth user")
    void getMyRecharges_inactiveUser_throws() {

        doThrow(new InactiveUserException(USER_ID))
                .when(userVerificationService)
                .verifyActiveUser(USER_ID, "CONSUMER");

        assertThatThrownBy(() -> rechargeService.getMyRecharges(
                USER_ID, "CONSUMER"))
                .isInstanceOf(InactiveUserException.class);

        verify(rechargeRepository, never())
                .findByUserIdOrderByCreatedAtDesc(any());
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private CreateRechargeOrderRequest validRequest(PaymentMethod method) {
        return validRequest(method, "500.00");
    }

    private CreateRechargeOrderRequest validRequest(
            PaymentMethod method, String amount) {

        return CreateRechargeOrderRequest.builder()
                .amount(new BigDecimal(amount))
                .currency(Currency.INR)
                .paymentMethod(method)
                .organizationId(ORG_ID)
                .build();
    }

    private RechargeTransaction buildRecharge(PaymentStatus status) {

        return RechargeTransaction.builder()
                .id(1L)
                .rechargeReference("RCH-ABC123")
                .orderId("order_1")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .userId(USER_ID)
                .organizationId(ORG_ID)
                .amount(new BigDecimal("500.00"))
                .currency(Currency.INR)
                .paymentMethod(PaymentMethod.UPI)
                .provider(PaymentProvider.RAZORPAY)
                .status(status)
                .build();
    }

    private String capturedPayload(
            String orderId, String paymentId, long amountPaise) {

        return """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "amount": %d,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """.formatted(paymentId, orderId, amountPaise);
    }

    private String failedPayload(String orderId, String paymentId) {

        return """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "amount": 50000,
                        "status": "failed"
                      }
                    }
                  }
                }
                """.formatted(paymentId, orderId);
    }

    private String authorizedPayload(String orderId, String paymentId) {

        return """
                {
                  "event": "payment.authorized",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "amount": 50000,
                        "status": "authorized"
                      }
                    }
                  }
                }
                """.formatted(paymentId, orderId);
    }

    private String orderPaidPayload(String orderId, long amountPaise) {

        return """
                {
                  "event": "order.paid",
                  "payload": {
                    "order": {
                      "entity": {
                        "id": "%s",
                        "amount": %d,
                        "currency": "INR",
                        "status": "paid"
                      }
                    }
                  }
                }
                """.formatted(orderId, amountPaise);
    }
}

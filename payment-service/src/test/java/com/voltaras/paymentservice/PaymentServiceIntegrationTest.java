package com.voltaras.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.paymentservice.client.AuthServiceClient;
import com.voltaras.paymentservice.client.BillServiceClient;
import com.voltaras.paymentservice.client.BillSnapshot;
import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.dto.response.AuthUserResponse;
import com.voltaras.paymentservice.provider.RazorpayCreateOrderRequest;
import com.voltaras.paymentservice.provider.RazorpayGatewayClient;
import com.voltaras.paymentservice.provider.RazorpayOrder;
import com.voltaras.paymentservice.entity.Wallet;
import com.voltaras.paymentservice.exception.UserNotFoundException;
import com.voltaras.paymentservice.repository.PaymentTransactionRepository;
import com.voltaras.paymentservice.repository.RechargeTransactionRepository;
import com.voltaras.paymentservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests: Spring context, H2 persistence, the real
 * controller and services. The Auth Service, Bill Service, Organization
 * Service and Razorpay gateway clients are mocked so no external service
 * is needed; the webhook payload parsing and wallet logic run for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceIntegrationTest {

    private static final BigDecimal BILL_TOTAL = new BigDecimal("355.04");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    @Autowired
    private RechargeTransactionRepository rechargeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    @MockitoBean
    private BillServiceClient billServiceClient;

    @MockitoBean
    private OrganizationServiceClient organizationServiceClient;

    @MockitoBean
    private RazorpayGatewayClient razorpayGatewayClient;

    @BeforeEach
    void cleanDatabase() {
        // The Spring context (and the in-memory H2 database) is shared
        // across test methods, so each test starts from empty tables.
        paymentRepository.deleteAll();
        rechargeRepository.deleteAll();
        walletRepository.deleteAll();

        // Every flow in these tests runs as user 100, who is verified
        // active against the Auth Service mock by default.
        when(authServiceClient.getInternalUser(100L))
                .thenReturn(AuthUserResponse.builder()
                        .userId(100L)
                        .email("consumer@example.com")
                        .fullName("Test Consumer")
                        .role("CONSUMER")
                        .active(true)
                        .build());
    }

    @Test
    @DisplayName("Application context loads with persistence")
    void contextLoads() {
        assertThat(paymentRepository).isNotNull();
        assertThat(walletRepository).isNotNull();
    }

    @Test
    @DisplayName("Recharge webhook credits the wallet; bill payment debits it and notifies Bill Service")
    void rechargeAndBillPaymentFlow() throws Exception {

        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_it1", 50000, "INR", "created", "RCH-IT1"));
        when(razorpayGatewayClient.verifyWebhookSignature(any(), any(), any()))
                .thenReturn(true);
        when(billServiceClient.getConsumerBill(1L, 100L))
                .thenReturn(new BillSnapshot(
                        1L, 100L, "GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        // 1. Create the recharge order (UPI).
        MvcResult order = mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order_it1"))
                .andReturn();

        // 2. Razorpay sends a captured webhook.
        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "any-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedWebhookBody("order_it1", "pay_it1")))
                .andExpect(status().isOk());

        // 3. The wallet now holds 500.00 INR.
        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        // 4. Replay the same webhook: idempotent, no double credit.
        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "any-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedWebhookBody("order_it1", "pay_it1")))
                .andExpect(status().isOk());

        // 5. Pay the full bill (355.04) from the wallet.
        MvcResult payment = mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-bill-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBillBody("355.04")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andReturn();

        JsonNode paymentBody = objectMapper.readTree(
                payment.getResponse().getContentAsString());

        long paymentId = paymentBody.get("id").asLong();

        // 6. The wallet is debited: 500.00 - 355.04 = 144.96.
        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(144.96));

        // The Bill Service was notified exactly once with PAID.
        verify(billServiceClient).notifyPaymentStatus(1L, "PAID", BILL_TOTAL);

        // The wallet row itself reflects the debit.
        Wallet wallet = walletRepository.findByUserId(100L).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("144.96");

        // 7. Recharge history and payment reads work.
        mockMvc.perform(get("/api/recharges/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/payments/" + paymentId)
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference")
                        .value(paymentBody.get("paymentReference").asText()));

        // 8. The recharge and payment rows are persisted.
        assertThat(rechargeRepository.findByOrderId("order_it1")).isPresent();
        assertThat(paymentRepository.findById(paymentId)).isPresent();
    }

    @Test
    @DisplayName("Bill payment with an empty wallet returns 400 INSUFFICIENT_WALLET_BALANCE")
    void billPaymentInsufficientBalance_returns400() throws Exception {

        when(billServiceClient.getConsumerBill(1L, 100L))
                .thenReturn(new BillSnapshot(
                        1L, 100L, "GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-bill-empty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBillBody("355.04")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INSUFFICIENT_WALLET_BALANCE"));

        verify(billServiceClient, never())
                .notifyPaymentStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Failed webhook does not credit the wallet")
    void failedWebhook_doesNotCredit() throws Exception {

        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_fail", 50000, "INR", "created", "RCH-FAIL"));
        when(razorpayGatewayClient.verifyWebhookSignature(any(), any(), any()))
                .thenReturn(true);

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "any-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedWebhookBody("order_fail", "pay_fail")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.00));

        // Recharge is persisted as FAILED.
        assertThat(rechargeRepository.findByOrderId("order_fail"))
                .get()
                .satisfies(r -> {
                    assertThat(r.getStatus().name()).isEqualTo("FAILED");
                    assertThat(r.getFailureCode())
                            .isEqualTo("RAZORPAY_PAYMENT_FAILED");
                });
    }

    @Test
    @DisplayName("Recharging with the same idempotency key does not create a duplicate order")
    void duplicateRechargeIdempotencyKey_returnsOriginal() throws Exception {

        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_dup", 50000, "INR", "created", "RCH-DUP"));

        MvcResult first = mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(
                first.getResponse().getContentAsString());
        JsonNode secondBody = objectMapper.readTree(
                second.getResponse().getContentAsString());

        assertThat(secondBody.get("orderId").asText())
                .isEqualTo(firstBody.get("orderId").asText());

        // The gateway was called exactly once.
        verify(razorpayGatewayClient, times(1))
                .createOrder(any(RazorpayCreateOrderRequest.class));
        assertThat(rechargeRepository.findByOrderId("order_dup")).isPresent();
    }

    @Test
    @DisplayName("Reusing the recharge idempotency key with different data returns 409")
    void rechargeIdempotencyConflict_returns409() throws Exception {

        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_conflict", 50000, "INR", "created", "RCH-CONF"));

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI").replace("500.00", "700.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @DisplayName("Repeating the bill payment idempotency key does not debit the wallet twice")
    void duplicateBillPaymentIdempotencyKey_returnsOriginal() throws Exception {

        when(razorpayGatewayClient.createOrder(any(RazorpayCreateOrderRequest.class)))
                .thenReturn(new RazorpayOrder(
                        "order_it2", 50000, "INR", "created", "RCH-IT2"));
        when(razorpayGatewayClient.verifyWebhookSignature(any(), any(), any()))
                .thenReturn(true);
        when(billServiceClient.getConsumerBill(1L, 100L))
                .thenReturn(new BillSnapshot(
                        1L, 100L, "GENERATED", "UNPAID",
                        BILL_TOTAL, BigDecimal.ZERO, BILL_TOTAL));

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "any-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedWebhookBody("order_it2", "pay_it2")))
                .andExpect(status().isOk());

        MvcResult first = mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-bill-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBillBody("355.04")))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-bill-dup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBillBody("355.04")))
                .andExpect(status().isCreated());

        JsonNode firstBody = objectMapper.readTree(
                first.getResponse().getContentAsString());

        // Wallet debited once: 500.00 - 355.04 = 144.96.
        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(144.96));

        verify(billServiceClient, times(1))
                .notifyPaymentStatus(1L, "PAID", BILL_TOTAL);
        assertThat(paymentRepository.findByBillIdOrderByCreatedAtDesc(1L))
                .hasSize(1);

        String reference = firstBody.get("paymentReference").asText();
        mockMvc.perform(get("/api/payments/reference/" + reference)
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Inactive auth user cannot create a recharge order (403 USER_INACTIVE)")
    void inactiveAuthUser_recharge_returns403() throws Exception {

        when(authServiceClient.getInternalUser(100L))
                .thenReturn(AuthUserResponse.builder()
                        .userId(100L)
                        .email("consumer@example.com")
                        .fullName("Test Consumer")
                        .role("CONSUMER")
                        .active(false)
                        .build());

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-recharge-inactive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_INACTIVE"));

        // No order was ever created at the gateway.
        verify(razorpayGatewayClient, never())
                .createOrder(any(RazorpayCreateOrderRequest.class));
    }

    @Test
    @DisplayName("Bill payment fails when the auth user is inactive (403 USER_INACTIVE)")
    void inactiveAuthUser_billPayment_returns403() throws Exception {

        when(authServiceClient.getInternalUser(100L))
                .thenReturn(AuthUserResponse.builder()
                        .userId(100L)
                        .email("consumer@example.com")
                        .fullName("Test Consumer")
                        .role("CONSUMER")
                        .active(false)
                        .build());

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "it-bill-inactive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBillBody("355.04")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_INACTIVE"));

        verify(billServiceClient, never())
                .getConsumerBill(any(), any());

        // The wallet was never touched or created.
        assertThat(walletRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Unknown auth user id returns 404 USER_NOT_FOUND")
    void unknownAuthUser_returns404() throws Exception {

        when(authServiceClient.getInternalUser(100L))
                .thenThrow(new UserNotFoundException(100L));

        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Webhook with a wrong signature is rejected with 403")
    void webhookWrongSignature_returns403() throws Exception {

        when(razorpayGatewayClient.verifyWebhookSignature(any(), any(), any()))
                .thenReturn(false);

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "wrong-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedWebhookBody("order_x", "pay_x")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("FORBIDDEN_OPERATION"));

        assertThat(walletRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Swagger/OpenAPI endpoints are available")
    void swaggerDocsAreAvailable() throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        // springdoc redirects /swagger-ui.html to /swagger-ui/index.html.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requests without the gateway headers are rejected with MISSING_HEADER")
    void missingHeaders_rejected() throws Exception {

        mockMvc.perform(post("/api/recharges/orders")
                        .header("Idempotency-Key", "it-no-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rechargeBody("UPI")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    // ==================================================================
    // Payload helpers
    // ==================================================================

    private String rechargeBody(String method) {
        return """
                {
                  "organizationId": 6,
                  "amount": 500.00,
                  "currency": "INR",
                  "paymentMethod": "%s"
                }
                """.formatted(method);
    }

    private String payBillBody(String amount) {
        return """
                {
                  "organizationId": 6,
                  "amount": %s,
                  "currency": "INR"
                }
                """.formatted(amount);
    }

    private String capturedWebhookBody(String orderId, String paymentId) {
        return """
                {
                  "entity": "event",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "amount": 50000,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """.formatted(paymentId, orderId);
    }

    private String failedWebhookBody(String orderId, String paymentId) {
        return """
                {
                  "entity": "event",
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "amount": 50000,
                        "currency": "INR",
                        "status": "failed"
                      }
                    }
                  }
                }
                """.formatted(paymentId, orderId);
    }
}

package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.config.SecurityConfig;
import com.voltaras.paymentservice.dto.request.CreateRechargeOrderRequest;
import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.service.RechargeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link RechargeController}.
 */
@WebMvcTest(RechargeController.class)
@Import(SecurityConfig.class)
class RechargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RechargeService rechargeService;

    @Test
    @DisplayName("POST create recharge order (UPI): 201 Created")
    void createRechargeOrder_upi_returns201() throws Exception {

        when(rechargeService.createRechargeOrder(
                eq(100L), eq("CONSUMER"), eq("key-upi"), any(
                        CreateRechargeOrderRequest.class)))
                .thenReturn(RechargeOrderResponse.builder()
                        .id(1L)
                        .rechargeReference("RCH-ABC123")
                        .orderId("order_test_upi")
                        .amount(new BigDecimal("500.00"))
                        .status(PaymentStatus.CREATED)
                        .razorpayKeyId("rzp_test_0000000000000000")
                        .build());

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-upi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upiBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order_test_upi"))
                .andExpect(jsonPath("$.razorpayKeyId")
                        .value("rzp_test_0000000000000000"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST create recharge order (CARD): 201 Created")
    void createRechargeOrder_card_returns201() throws Exception {

        when(rechargeService.createRechargeOrder(
                eq(100L), eq("CONSUMER"), eq("key-card"), any(
                        CreateRechargeOrderRequest.class)))
                .thenReturn(RechargeOrderResponse.builder()
                        .id(2L)
                        .rechargeReference("RCH-CARD123")
                        .orderId("order_test_card")
                        .amount(new BigDecimal("250.00"))
                        .status(PaymentStatus.CREATED)
                        .build());

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order_test_card"));
    }

    @Test
    @DisplayName("POST create recharge order: missing Idempotency-Key returns 400")
    void createRechargeOrder_missingKey_returns400() throws Exception {

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upiBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("POST create recharge order: invalid enum returns 400 MALFORMED_REQUEST")
    void createRechargeOrder_invalidMethod_returns400() throws Exception {

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upiBody().replace("\"UPI\"", "\"CHEQUE\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("POST create recharge order: invalid amount returns 400 VALIDATION_ERROR")
    void createRechargeOrder_invalidAmount_returns400() throws Exception {

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upiBody().replace("500.00", "-50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("amount"));
    }

    @Test
    @DisplayName("POST create recharge order: WALLET method rejected by service (400 BAD_REQUEST)")
    void createRechargeOrder_walletMethod_returns400() throws Exception {

        when(rechargeService.createRechargeOrder(
                eq(100L), eq("CONSUMER"), eq("key-1"), any(
                        CreateRechargeOrderRequest.class)))
                .thenThrow(new BadRequestException(
                        "Recharge payment method must be UPI or CARD"));

        mockMvc.perform(post("/api/recharges/orders")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upiBody().replace("\"UPI\"", "\"WALLET\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("GET my recharges: 200 OK with history")
    void getMyRecharges_returns200() throws Exception {

        when(rechargeService.getMyRecharges(100L, "CONSUMER"))
                .thenReturn(List.of(
                        RechargeTransactionResponse.builder()
                                .id(1L)
                                .status(PaymentStatus.SUCCESS)
                                .build()));

        mockMvc.perform(get("/api/recharges/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    private String upiBody() {
        return """
                {
                  "organizationId": 6,
                  "amount": 500.00,
                  "currency": "INR",
                  "paymentMethod": "UPI"
                }
                """;
    }

    private String cardBody() {
        return """
                {
                  "organizationId": 6,
                  "amount": 250.00,
                  "currency": "INR",
                  "paymentMethod": "CARD"
                }
                """;
    }
}

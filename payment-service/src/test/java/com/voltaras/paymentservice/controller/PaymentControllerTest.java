package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.config.SecurityConfig;
import com.voltaras.paymentservice.dto.response.PaymentResponse;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.ResourceNotFoundException;
import com.voltaras.paymentservice.service.PaymentService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link PaymentController}.
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST pay bill from wallet: 201 Created with Location header")
    void payBillFromWallet_returns201Created() throws Exception {

        PaymentResponse response = PaymentResponse.builder()
                .id(10L)
                .paymentReference("PAY-ABC123")
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("223.13"))
                .build();

        when(paymentService.payBillFromWallet(
                eq(100L), eq("CONSUMER"), eq(1L), eq("key-1"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/payments/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST pay bill: missing Idempotency-Key returns 400 MISSING_HEADER")
    void payBill_missingIdempotencyKey_returns400() throws Exception {

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("POST pay bill: missing X-User-Id returns 400 MISSING_HEADER")
    void payBill_missingUserHeader_returns400() throws Exception {

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("POST pay bill: invalid amount returns 400 VALIDATION_ERROR")
    void payBill_invalidAmount_returns400() throws Exception {

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("223.13", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("amount"));
    }

    @Test
    @DisplayName("POST pay bill: invalid enum returns 400 MALFORMED_REQUEST")
    void payBill_invalidCurrency_returns400() throws Exception {

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("\"INR\"", "\"USD\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("POST pay bill: forbidden organization access returns 403")
    void payBill_forbiddenOrganization_returns403() throws Exception {

        when(paymentService.payBillFromWallet(
                eq(100L), eq("CONSUMER"), eq(1L), eq("key-1"), any()))
                .thenThrow(new ForbiddenOperationException(
                        "You are not an active member of this organization"));

        mockMvc.perform(post("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_OPERATION"));
    }

    @Test
    @DisplayName("GET payment by id: 200 OK")
    void getPaymentById_returns200() throws Exception {

        when(paymentService.getPaymentById(100L, "CONSUMER", 1L))
                .thenReturn(PaymentResponse.builder()
                        .id(1L)
                        .paymentReference("PAY-ABC123")
                        .status(PaymentStatus.SUCCESS)
                        .build());

        mockMvc.perform(get("/api/payments/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value("PAY-ABC123"));
    }

    @Test
    @DisplayName("GET payment by id: missing payment returns 404")
    void getPaymentById_missing_returns404() throws Exception {

        when(paymentService.getPaymentById(100L, "CONSUMER", 99L))
                .thenThrow(new ResourceNotFoundException("Payment", "id", 99L));

        mockMvc.perform(get("/api/payments/99")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET payment by reference: 200 OK")
    void getPaymentByReference_returns200() throws Exception {

        when(paymentService.getPaymentByReference(
                100L, "CONSUMER", "PAY-ABC123"))
                .thenReturn(PaymentResponse.builder()
                        .id(1L)
                        .paymentReference("PAY-ABC123")
                        .build());

        mockMvc.perform(get("/api/payments/reference/PAY-ABC123")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET bill payments: 200 OK with list")
    void getPaymentsForBill_returns200() throws Exception {

        when(paymentService.getPaymentsForBill(100L, "CONSUMER", 1L))
                .thenReturn(List.of(
                        PaymentResponse.builder().id(1L).build(),
                        PaymentResponse.builder().id(2L).build()));

        mockMvc.perform(get("/api/bills/1/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET my payments: 200 OK")
    void getMyPayments_returns200() throws Exception {

        mockMvc.perform(get("/api/payments")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk());
    }

    private String validBody() {
        return """
                {
                  "amount": 223.13,
                  "currency": "INR",
                  "organizationId": 6
                }
                """;
    }
}

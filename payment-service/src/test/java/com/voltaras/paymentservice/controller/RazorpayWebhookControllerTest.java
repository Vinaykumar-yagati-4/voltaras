package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.config.SecurityConfig;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.service.RechargeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link RazorpayWebhookController}.
 */
@WebMvcTest(RazorpayWebhookController.class)
@Import(SecurityConfig.class)
class RazorpayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RechargeService rechargeService;

    @Test
    @DisplayName("POST Razorpay webhook: 200 OK when the signature verifies")
    void webhook_validSignature_returns200() throws Exception {

        doNothing().when(rechargeService)
                .handleRazorpayWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST Razorpay webhook: 403 when the signature is invalid")
    void webhook_invalidSignature_returns403() throws Exception {

        doThrow(new ForbiddenOperationException(
                "Invalid Razorpay webhook signature"))
                .when(rechargeService)
                .handleRazorpayWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("FORBIDDEN_OPERATION"));
    }

    private String webhookBody() {
        return """
                {
                  "entity": "event",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test1",
                        "order_id": "order_test1",
                        "amount": 50000,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;
    }
}

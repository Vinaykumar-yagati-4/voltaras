package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.config.SecurityConfig;
import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link WalletController}.
 */
@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    @DisplayName("GET my wallet: 200 OK with balance")
    void getMyWallet_returns200() throws Exception {

        when(walletService.getMyWallet(100L, "CONSUMER"))
                .thenReturn(WalletResponse.builder()
                        .id(1L)
                        .userId(100L)
                        .balance(new BigDecimal("1500.00"))
                        .currency(Currency.INR)
                        .build());

        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.userId").value(100));
    }

    @Test
    @DisplayName("GET my wallet: missing X-User-Id returns 400")
    void getMyWallet_missingUserHeader_returns400() throws Exception {

        mockMvc.perform(get("/api/wallet/me")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }
}

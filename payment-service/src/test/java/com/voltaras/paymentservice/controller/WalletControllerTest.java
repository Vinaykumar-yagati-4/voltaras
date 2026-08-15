package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.config.SecurityConfig;
import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.service.RechargeService;
import com.voltaras.paymentservice.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private RechargeService rechargeService;

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

    @Test
    @DisplayName("POST top-up: with organizationId persists a local recharge and credits the wallet")
    void topUp_withOrganizationId_recordsLocalRecharge() throws Exception {

        when(walletService.getMyWallet(100L, "CONSUMER"))
                .thenReturn(WalletResponse.builder()
                        .id(1L)
                        .userId(100L)
                        .balance(new BigDecimal("1600.00"))
                        .currency(Currency.INR)
                        .build());

        mockMvc.perform(post("/api/wallet/top-up")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "amount": 100.00,
                                  "organizationId": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1600.00));

        verify(rechargeService).recordLocalRecharge(
                100L, "CONSUMER", 6L, new BigDecimal("100.00"));
        verify(walletService, never()).credit(any(), any());
    }

    @Test
    @DisplayName("POST top-up: without organizationId credits the wallet directly")
    void topUp_withoutOrganizationId_creditsOnly() throws Exception {

        mockMvc.perform(post("/api/wallet/top-up")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "amount": 250.00
                                }
                                """))
                .andExpect(status().isOk());

        verify(walletService).credit(100L, new BigDecimal("250.00"));
        verify(rechargeService, never())
                .recordLocalRecharge(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST top-up: zero amount returns 400")
    void topUp_zeroAmount_returns400() throws Exception {

        mockMvc.perform(post("/api/wallet/top-up")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(walletService, never()).credit(any(), any());
        verify(rechargeService, never())
                .recordLocalRecharge(any(), any(), any(), any());
    }
}

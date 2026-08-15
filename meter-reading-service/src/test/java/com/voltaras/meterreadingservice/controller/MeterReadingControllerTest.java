package com.voltaras.meterreadingservice.controller;

import com.voltaras.meterreadingservice.dto.request.CreateAdminMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import com.voltaras.meterreadingservice.service.MeterReadingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link MeterReadingController}: header binding,
 * HTTP status codes and validation failures.
 */
@WebMvcTest(MeterReadingController.class)
class MeterReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterReadingService meterReadingService;

    // ------------------------------------------------------------------
    // Consumer endpoints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST submit: 201 Created with response body")
    void submitReading_returns201Created() throws Exception {

        MeterReadingResponse response = MeterReadingResponse.builder()
                .id(1L)
                .authUserId(100L)
                .meterNumber("MTR-001")
                .billingMonth(7)
                .billingYear(2026)
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .unitsConsumed(new BigDecimal("100.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .status(MeterReadingStatus.SUBMITTED)
                .build();

        when(meterReadingService.submitReading(eq(100L), any(SubmitMeterReadingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/meter-readings")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "billingMonth": 7,
                                  "billingYear": 2026,
                                  "previousReading": 900.000,
                                  "currentReading": 1000.000,
                                  "readingDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(100))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST submit: validation failure returns 400 with field errors")
    void submitReading_validationFailure_returns400WithFieldErrors() throws Exception {

        mockMvc.perform(post("/api/meter-readings")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "",
                                  "previousReading": -5,
                                  "currentReading": 10,
                                  "readingDate": "2099-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'meterNumber')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Meter number is required")))
                .andExpect(jsonPath("$.error.details[?(@.field == 'previousReading')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Previous reading must be zero or positive")))
                .andExpect(jsonPath("$.error.details[?(@.field == 'readingDate')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Reading date cannot be in the future")));
    }

    @Test
    @DisplayName("POST submit: missing X-User-Id header returns 400")
    void submitReading_missingHeader_returns400() throws Exception {

        mockMvc.perform(post("/api/meter-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "billingMonth": 7,
                                  "billingYear": 2026,
                                  "previousReading": 900,
                                  "currentReading": 1000,
                                  "readingDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("GET /me: returns the consumer's readings with 200 OK")
    void getMyReadings_returns200Ok() throws Exception {

        MeterReadingResponse response = MeterReadingResponse.builder()
                .id(1L)
                .authUserId(100L)
                .meterNumber("MTR-001")
                .status(MeterReadingStatus.SUBMITTED)
                .build();

        when(meterReadingService.getMyReadings(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/meter-readings/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("DELETE /me/{id}: returns 200 with clean message")
    void deleteMyReading_returns200WithMessage() throws Exception {

        mockMvc.perform(delete("/api/meter-readings/me/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meter reading deleted successfully"));

        verify(meterReadingService).deleteMyReading(100L, 1L);
    }

    // ------------------------------------------------------------------
    // Admin endpoints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST admin: 201 Created with reading recorded for the consumer")
    void createReadingForAdmin_returns201Created() throws Exception {

        MeterReadingResponse response = MeterReadingResponse.builder()
                .id(1L)
                .authUserId(100L)
                .meterNumber("MTR-001")
                .billingMonth(7)
                .billingYear(2026)
                .previousReading(new BigDecimal("900.000"))
                .currentReading(new BigDecimal("1000.000"))
                .unitsConsumed(new BigDecimal("100.000"))
                .readingDate(LocalDate.of(2026, 7, 31))
                .status(MeterReadingStatus.SUBMITTED)
                .build();

        when(meterReadingService.createReadingForAdmin(
                eq(1L), eq("ADMIN"), any(CreateAdminMeterReadingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/meter-readings/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authUserId": 100,
                                  "meterNumber": "MTR-001",
                                  "previousReading": 900.000,
                                  "currentReading": 1000.000,
                                  "readingDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(100))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST admin: missing authUserId fails validation with 400")
    void createReadingForAdmin_missingAuthUserId_returns400() throws Exception {

        mockMvc.perform(post("/api/meter-readings/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "previousReading": 900,
                                  "currentReading": 1000,
                                  "readingDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH admin verify: 200 OK")
    void verifyReading_returns200Ok() throws Exception {

        MeterReadingResponse response = MeterReadingResponse.builder()
                .id(1L)
                .status(MeterReadingStatus.VERIFIED)
                .build();

        when(meterReadingService.verifyReading(1L, "ADMIN", 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/meter-readings/admin/1/verify")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    @DisplayName("PATCH admin reject: 200 OK")
    void rejectReading_returns200Ok() throws Exception {

        MeterReadingResponse response = MeterReadingResponse.builder()
                .id(1L)
                .status(MeterReadingStatus.REJECTED)
                .build();

        when(meterReadingService.rejectReading(eq(1L), eq("ADMIN"), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/meter-readings/admin/1/reject")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remarks": "Meter reading exceeds expected consumption"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH admin reject: blank remarks fails validation with 400")
    void rejectReading_blankRemarks_returns400() throws Exception {

        mockMvc.perform(patch("/api/meter-readings/admin/1/reject")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remarks": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET admin: invalid status query parameter returns 400")
    void getAllReadingsForAdmin_invalidStatus_returns400() throws Exception {

        mockMvc.perform(get("/api/meter-readings/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET admin: authUserId query parameter is forwarded to the service")
    void getAllReadingsForAdmin_authUserId_forwardsToService() throws Exception {

        when(meterReadingService.getAllReadingsForAdmin("ADMIN", 66L, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/meter-readings/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .param("authUserId", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(meterReadingService).getAllReadingsForAdmin("ADMIN", 66L, null);
    }
}

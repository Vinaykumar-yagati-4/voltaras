package com.voltaras.metermanagementservice.controller;

import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import com.voltaras.metermanagementservice.exception.AccessDeniedException;
import com.voltaras.metermanagementservice.service.MeterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link MeterAdminController}: ADMIN role headers,
 * HTTP status codes and response bodies.
 */
@WebMvcTest(MeterAdminController.class)
class MeterAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterService meterService;

    @Test
    @DisplayName("POST create: 201 Created with response body")
    void createMeter_returns201Created() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .meterType(MeterType.SMART)
                .connectionType(ConnectionType.RESIDENTIAL)
                .phaseType(PhaseType.SINGLE_PHASE)
                .status(MeterStatus.ACTIVE)
                .sanctionedLoadKw(new BigDecimal("5.000"))
                .build();

        when(meterService.createMeter(eq(1L), eq("ADMIN"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "meterType": "SMART",
                                  "connectionType": "RESIDENTIAL",
                                  "phaseType": "SINGLE_PHASE",
                                  "sanctionedLoadKw": 5.000,
                                  "city": "Bengaluru",
                                  "pincode": "560001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.meterNumber").value("MTR-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST create: non-admin role receives 403 ACCESS_DENIED")
    void createMeter_nonAdminRole_returns403() throws Exception {

        when(meterService.createMeter(eq(1L), eq("CONSUMER"), any()))
                .thenThrow(new AccessDeniedException(
                        "Only ADMIN users can perform this operation"));

        mockMvc.perform(post("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "meterType": "SMART",
                                  "connectionType": "RESIDENTIAL",
                                  "phaseType": "SINGLE_PHASE",
                                  "sanctionedLoadKw": 5.0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET list: filters are forwarded and 200 OK returned")
    void getAllMetersForAdmin_returns200Ok() throws Exception {

        MeterSummaryResponse summary = MeterSummaryResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .status(MeterStatus.ACTIVE)
                .build();

        when(meterService.getAllMetersForAdmin(
                eq("ADMIN"), eq(MeterStatus.ACTIVE), eq(100L), eq(7L), eq("MTR-001")))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .param("status", "ACTIVE")
                        .param("authUserId", "100")
                        .param("organizationId", "7")
                        .param("meterNumber", "MTR-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].meterNumber").value("MTR-001"));
    }

    @Test
    @DisplayName("GET list: invalid status filter returns 400 INVALID_ARGUMENT")
    void getAllMetersForAdmin_invalidStatus_returns400() throws Exception {

        mockMvc.perform(get("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET by id: 200 OK")
    void getMeterByIdForAdmin_returns200Ok() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .status(MeterStatus.ACTIVE)
                .build();

        when(meterService.getMeterByIdForAdmin("ADMIN", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/meters/admin/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meterNumber").value("MTR-001"));
    }

    @Test
    @DisplayName("PUT update: 200 OK")
    void updateMeter_returns200Ok() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .city("Chennai")
                .status(MeterStatus.ACTIVE)
                .build();

        when(meterService.updateMeter(eq("ADMIN"), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/meters/admin/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Chennai",
                                  "sanctionedLoadKw": 10.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Chennai"));
    }

    @Test
    @DisplayName("PATCH assign: 200 OK with assigned user")
    void assignMeter_returns200Ok() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .authUserId(100L)
                .organizationId(7L)
                .status(MeterStatus.ACTIVE)
                .build();

        when(meterService.assignMeter(eq("ADMIN"), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/meters/admin/1/assign")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authUserId": 100,
                                  "organizationId": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(100))
                .andExpect(jsonPath("$.organizationId").value(7));
    }

    @Test
    @DisplayName("PATCH status: 200 OK with new status")
    void updateMeterStatus_returns200Ok() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .status(MeterStatus.FAULTY)
                .build();

        when(meterService.updateMeterStatus(eq("ADMIN"), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/meters/admin/1/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FAULTY",
                                  "remarks": "Display not working"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAULTY"));
    }

    @Test
    @DisplayName("DELETE: soft delete returns 200 with message")
    void removeMeter_returns200WithMessage() throws Exception {

        mockMvc.perform(delete("/api/meters/admin/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meter removed successfully"));

        verify(meterService).removeMeter("ADMIN", 1L);
    }
}

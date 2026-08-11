package com.voltaras.metermanagementservice.controller;

import com.voltaras.metermanagementservice.dto.response.MeterResponse;
import com.voltaras.metermanagementservice.dto.response.MeterSummaryResponse;
import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import com.voltaras.metermanagementservice.exception.ResourceNotFoundException;
import com.voltaras.metermanagementservice.service.MeterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link MeterController}: header binding, ownership
 * scoping and error responses.
 */
@WebMvcTest(MeterController.class)
class MeterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterService meterService;

    @Test
    @DisplayName("GET /api/meters: returns the user's meters with 200 OK")
    void getMyMeters_returns200Ok() throws Exception {

        MeterSummaryResponse summary = MeterSummaryResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .authUserId(100L)
                .meterType(MeterType.SMART)
                .connectionType(ConnectionType.RESIDENTIAL)
                .phaseType(PhaseType.SINGLE_PHASE)
                .status(MeterStatus.ACTIVE)
                .sanctionedLoadKw(new BigDecimal("5.000"))
                .city("Bengaluru")
                .build();

        when(meterService.getMyMeters(100L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/meters")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].meterNumber").value("MTR-001"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/meters: missing X-User-Id header returns 400")
    void getMyMeters_missingHeader_returns400() throws Exception {

        mockMvc.perform(get("/api/meters"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("GET /api/meters/{id}: owned meter returned with 200 OK")
    void getMyMeterById_returns200Ok() throws Exception {

        MeterResponse response = MeterResponse.builder()
                .id(1L)
                .meterNumber("MTR-001")
                .authUserId(100L)
                .status(MeterStatus.ACTIVE)
                .build();

        when(meterService.getMyMeterById(100L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/meters/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(100));
    }

    @Test
    @DisplayName("GET /api/meters/{id}: foreign meter maps to 404 RESOURCE_NOT_FOUND")
    void getMyMeterById_foreignMeter_returns404() throws Exception {

        when(meterService.getMyMeterById(100L, 99L))
                .thenThrow(new ResourceNotFoundException("Meter", "id", 99L));

        mockMvc.perform(get("/api/meters/99")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}

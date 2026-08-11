package com.voltaras.metermanagementservice.controller;

import com.voltaras.metermanagementservice.exception.DuplicateResourceException;
import com.voltaras.metermanagementservice.service.MeterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validation and error-scenario tests shared by the user and admin
 * controllers: bean validation, malformed bodies, missing headers and
 * duplicate-resource handling.
 */
@WebMvcTest({MeterController.class, MeterAdminController.class})
class MeterApiValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeterService meterService;

    @Test
    @DisplayName("POST create: missing meterNumber, negative load and bad pincode fail validation")
    void createMeter_validationFailure_returns400WithFieldErrors() throws Exception {

        mockMvc.perform(post("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "",
                                  "meterType": "SMART",
                                  "connectionType": "RESIDENTIAL",
                                  "phaseType": "SINGLE_PHASE",
                                  "sanctionedLoadKw": -5,
                                  "pincode": "12"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'meterNumber')].message")
                        .value(org.hamcrest.Matchers.hasItem("Meter number is required")))
                .andExpect(jsonPath("$.error.details[?(@.field == 'sanctionedLoadKw')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Sanctioned load must be positive")))
                .andExpect(jsonPath("$.error.details[?(@.field == 'pincode')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Pincode must be a valid 6-digit postal code")));
    }

    @Test
    @DisplayName("POST create: invalid enum in body returns 400 MALFORMED_REQUEST")
    void createMeter_invalidEnum_returns400() throws Exception {

        mockMvc.perform(post("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterNumber": "MTR-001",
                                  "meterType": "NUCLEAR",
                                  "connectionType": "RESIDENTIAL",
                                  "phaseType": "SINGLE_PHASE",
                                  "sanctionedLoadKw": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("POST create: malformed JSON returns 400 MALFORMED_REQUEST")
    void createMeter_malformedJson_returns400() throws Exception {

        mockMvc.perform(post("/api/meters/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("POST create: duplicate meter number returns 409 DUPLICATE_RESOURCE")
    void createMeter_duplicateMeterNumber_returns409() throws Exception {

        when(meterService.createMeter(eq(1L), eq("ADMIN"), any()))
                .thenThrow(new DuplicateResourceException(
                        "Meter", "meterNumber", "MTR-001"));

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
                                  "sanctionedLoadKw": 5
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("PATCH assign: missing authUserId fails validation")
    void assignMeter_missingAuthUserId_returns400() throws Exception {

        mockMvc.perform(patch("/api/meters/admin/1/assign")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'authUserId')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "authUserId is required when assigning a meter")));
    }

    @Test
    @DisplayName("PATCH status: missing status fails validation")
    void updateMeterStatus_missingStatus_returns400() throws Exception {

        mockMvc.perform(patch("/api/meters/admin/1/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remarks": "No status provided"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Admin endpoint: missing X-User-Role header returns 400 MISSING_HEADER")
    void adminEndpoint_missingRoleHeader_returns400() throws Exception {

        mockMvc.perform(get("/api/meters/admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }
}

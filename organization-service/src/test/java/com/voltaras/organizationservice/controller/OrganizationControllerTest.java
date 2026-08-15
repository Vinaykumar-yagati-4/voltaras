package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.config.SecurityConfig;
import com.voltaras.organizationservice.dto.response.AvailableOrganizationResponse;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.service.OrganizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link OrganizationController}: header binding,
 * HTTP status codes and validation failures.
 */
@WebMvcTest(OrganizationController.class)
@Import(SecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @Test
    @DisplayName("POST create: 201 Created with response body")
    void createOrganization_returns201Created() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .name("Sunrise Hostel")
                .organizationCode("SUNRISE-HST")
                .organizationType(OrganizationType.HOSTEL)
                .status(OrganizationStatus.ACTIVE)
                .build();

        when(organizationService.createOrganization(eq(100L), any())).thenReturn(response);

        mockMvc.perform(post("/api/organizations")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sunrise Hostel",
                                  "organizationCode": "SUNRISE-HST",
                                  "organizationType": "HOSTEL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.organizationCode").value("SUNRISE-HST"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST create: blank name fails validation with 400")
    void createOrganization_validationFailure_returns400() throws Exception {

        mockMvc.perform(post("/api/organizations")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "organizationCode": "SUNRISE-HST",
                                  "organizationType": "HOSTEL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'name')].message")
                        .value(org.hamcrest.Matchers.hasItem(
                                "Organization name is required")));
    }

    @Test
    @DisplayName("GET available: 200 OK with ACTIVE organizations")
    void getAvailableOrganizations_returns200Ok() throws Exception {

        AvailableOrganizationResponse response = AvailableOrganizationResponse.builder()
                .id(1L)
                .name("Voltaras Demo Society")
                .organizationCode("VOLTARAS_DEMO")
                .organizationType(OrganizationType.APARTMENT)
                .city("Hyderabad")
                .build();

        when(organizationService.getAvailableOrganizations(100L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/organizations/available")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].organizationCode").value("VOLTARAS_DEMO"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("POST create: missing X-User-Id header returns 400")
    void createOrganization_missingHeader_returns400() throws Exception {

        mockMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sunrise Hostel",
                                  "organizationCode": "SUNRISE-HST",
                                  "organizationType": "HOSTEL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("POST create: unknown organizationType fails validation")
    void createOrganization_invalidType_returns400() throws Exception {

        mockMvc.perform(post("/api/organizations")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sunrise Hostel",
                                  "organizationCode": "SUNRISE-HST",
                                  "organizationType": "INDIVIDUAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("GET /me: returns my organizations with 200 OK")
    void getMyOrganizations_returns200Ok() throws Exception {

        MembershipResponse response = MembershipResponse.builder()
                .id(1L)
                .organizationId(1L)
                .organizationName("Sunrise Hostel")
                .authUserId(100L)
                .membershipRole(MembershipRole.OWNER)
                .build();

        when(organizationService.getMyOrganizations(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/organizations/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].membershipRole").value("OWNER"));
    }

    @Test
    @DisplayName("GET by id: 200 OK")
    void getOrganizationById_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .name("Sunrise Hostel")
                .build();

        when(organizationService.getOrganizationById(100L, "CONSUMER", 1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/organizations/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT update: 200 OK")
    void updateOrganization_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .name("Sunrise Hostel (Renovated)")
                .build();

        when(organizationService.updateOrganization(eq(100L), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/organizations/1")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sunrise Hostel (Renovated)",
                                  "organizationType": "HOSTEL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sunrise Hostel (Renovated)"));
    }

    @Test
    @DisplayName("PATCH deactivate: 200 OK with INACTIVE status")
    void deactivateOrganization_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .status(OrganizationStatus.INACTIVE)
                .build();

        when(organizationService.deactivateOrganization(100L, "CONSUMER", 1L))
                .thenReturn(response);

        mockMvc.perform(patch("/api/organizations/1/deactivate")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}

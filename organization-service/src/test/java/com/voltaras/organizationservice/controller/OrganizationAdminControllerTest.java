package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.config.SecurityConfig;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.service.OrganizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link OrganizationAdminController}: system-ADMIN
 * listing, suspend and activate operations.
 */
@WebMvcTest(OrganizationAdminController.class)
@Import(SecurityConfig.class)
class OrganizationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @Test
    @DisplayName("GET admin list: 200 OK with paginated list")
    void getAllOrganizationsForAdmin_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .name("Sunrise Hostel")
                .status(OrganizationStatus.ACTIVE)
                .build();

        when(organizationService.getAllOrganizationsForAdmin(
                eq("ADMIN"),
                isNull(),
                isNull(),
                any()
        )).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/admin/organizations")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("GET admin list: invalid status parameter returns 400")
    void getAllOrganizationsForAdmin_invalidStatus_returns400() throws Exception {

        mockMvc.perform(get("/api/admin/organizations")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET admin by id: 200 OK")
    void getOrganizationForAdmin_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .build();

        when(organizationService.getOrganizationForAdmin(
                eq("ADMIN"),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(get("/api/admin/organizations/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH suspend: 200 OK with SUSPENDED status")
    void suspendOrganizationForAdmin_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .status(OrganizationStatus.SUSPENDED)
                .build();

        when(organizationService.suspendOrganizationForAdmin(
                eq("ADMIN"),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/admin/organizations/1/suspend")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PATCH activate: 200 OK with ACTIVE status")
    void activateOrganizationForAdmin_returns200Ok() throws Exception {

        OrganizationResponse response = OrganizationResponse.builder()
                .id(1L)
                .status(OrganizationStatus.ACTIVE)
                .build();

        when(organizationService.activateOrganizationForAdmin(
                eq("ADMIN"),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/admin/organizations/1/activate")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
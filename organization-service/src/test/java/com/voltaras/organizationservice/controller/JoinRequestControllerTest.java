package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.config.SecurityConfig;
import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.service.JoinRequestService;
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
 * Web-layer tests for {@link JoinRequestController}: create, list, approve,
 * reject validation and cancel.
 */
@WebMvcTest(JoinRequestController.class)
@Import(SecurityConfig.class)
class JoinRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JoinRequestService joinRequestService;

    @Test
    @DisplayName("POST join request: 201 Created with PENDING status")
    void createJoinRequest_returns201Created() throws Exception {

        JoinRequestResponse response = JoinRequestResponse.builder()
                .id(5L)
                .organizationId(1L)
                .authUserId(100L)
                .requestedRole(MembershipRole.TENANT)
                .status(JoinRequestStatus.PENDING)
                .build();

        when(joinRequestService.createJoinRequest(eq(100L), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/1/join-requests")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedRole": "TENANT",
                                  "requestMessage": "I would like to join the hostel"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedRole").value("TENANT"));
    }

    @Test
    @DisplayName("GET join requests: 200 OK")
    void getOrganizationJoinRequests_returns200Ok() throws Exception {

        JoinRequestResponse response = JoinRequestResponse.builder()
                .id(5L)
                .status(JoinRequestStatus.PENDING)
                .build();

        when(joinRequestService.getOrganizationJoinRequests(1L, 1L, null))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/organizations/1/join-requests")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET join requests: invalid status parameter returns 400")
    void getOrganizationJoinRequests_invalidStatus_returns400() throws Exception {

        mockMvc.perform(get("/api/organizations/1/join-requests")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET /join-requests/me: 200 OK")
    void getMyJoinRequests_returns200Ok() throws Exception {

        when(joinRequestService.getMyJoinRequests(100L))
                .thenReturn(List.of(JoinRequestResponse.builder().id(5L).build()));

        mockMvc.perform(get("/api/organizations/join-requests/me")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PATCH approve: 200 OK")
    void approveJoinRequest_returns200Ok() throws Exception {

        JoinRequestResponse response = JoinRequestResponse.builder()
                .id(5L)
                .status(JoinRequestStatus.APPROVED)
                .build();

        when(joinRequestService.approveJoinRequest(1L, 1L, 5L)).thenReturn(response);

        mockMvc.perform(patch("/api/organizations/1/join-requests/5/approve")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH reject: blank remarks fails validation with 400")
    void rejectJoinRequest_blankRemarks_returns400() throws Exception {

        mockMvc.perform(patch("/api/organizations/1/join-requests/5/reject")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
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
    @DisplayName("PATCH cancel: 200 OK")
    void cancelJoinRequest_returns200Ok() throws Exception {

        JoinRequestResponse response = JoinRequestResponse.builder()
                .id(5L)
                .status(JoinRequestStatus.CANCELLED)
                .build();

        when(joinRequestService.cancelJoinRequest(100L, 1L, 5L)).thenReturn(response);

        mockMvc.perform(patch("/api/organizations/1/join-requests/5/cancel")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

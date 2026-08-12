package com.voltaras.complaintservice.controller;

import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.StatusUpdateResponse;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.exception.AccessDeniedException;
import com.voltaras.complaintservice.exception.BadRequestException;
import com.voltaras.complaintservice.service.ComplaintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AdminComplaintController}.
 */
@WebMvcTest(AdminComplaintController.class)
class AdminComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintService complaintService;

    @Test
    @DisplayName("GET /api/admin/complaints: returns the paginated complaint queue")
    void getAllComplaints_returns200() throws Exception {

        when(complaintService.getAllComplaintsForAdmin(
                eq("ADMIN"), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/admin/complaints")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/complaints: missing X-User-Role header returns 400")
    void getAllComplaints_missingRole_returns400() throws Exception {

        mockMvc.perform(get("/api/admin/complaints")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("GET /api/admin/complaints: CONSUMER role maps to 403 ACCESS_DENIED")
    void getAllComplaints_consumerRole_returns403() throws Exception {

        when(complaintService.getAllComplaintsForAdmin(
                eq("CONSUMER"), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException(
                        "Only ADMIN users can perform this operation"));

        mockMvc.perform(get("/api/admin/complaints")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/complaints/{id}/status: valid transition returns 200")
    void updateStatus_returns200() throws Exception {

        when(complaintService.updateComplaintStatus(
                eq("ADMIN"), eq(1L), eq(5L), any()))
                .thenReturn(StatusUpdateResponse.builder()
                        .complaintId(5L)
                        .ticketNumber("CMP-20260812-0001")
                        .previousStatus(ComplaintStatus.OPEN)
                        .currentStatus(ComplaintStatus.IN_PROGRESS)
                        .updatedAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(patch("/api/admin/complaints/5/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus").value("OPEN"))
                .andExpect(jsonPath("$.currentStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /api/admin/complaints/{id}/status: invalid transition returns 400")
    void updateStatus_invalidTransition_returns400() throws Exception {

        when(complaintService.updateComplaintStatus(
                eq("ADMIN"), eq(1L), eq(5L), any()))
                .thenThrow(new BadRequestException(
                        "Invalid status transition from OPEN to CLOSED"));

        mockMvc.perform(patch("/api/admin/complaints/5/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("PATCH /api/admin/complaints/{id}/status: missing status body returns 400 VALIDATION_ERROR")
    void updateStatus_missingStatus_returns400() throws Exception {

        mockMvc.perform(patch("/api/admin/complaints/5/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/admin/complaints/{id}/assign: returns the assigned complaint")
    void assignComplaint_returns200() throws Exception {

        when(complaintService.assignComplaint(eq("ADMIN"), eq(1L), eq(5L), any()))
                .thenReturn(buildDetail());

        mockMvc.perform(put("/api/admin/complaints/5/assign")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedTo": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(2));
    }

    @Test
    @DisplayName("POST /api/admin/complaints/{id}/comments: returns 201")
    void addAdminComment_returns201() throws Exception {

        mockMvc.perform(post("/api/admin/complaints/5/comments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentText": "The meter reading was corrected."}
                                """))
                .andExpect(status().isCreated());
    }

    private ComplaintDetailResponse buildDetail() {

        return ComplaintDetailResponse.builder()
                .id(5L)
                .ticketNumber("CMP-20260812-0001")
                .consumerId(13L)
                .categoryId(1L)
                .categoryName("BILLING_ISSUE")
                .subject("Incorrect bill amount for July 2026")
                .description("My bill shows 350 units consumed but I only used about 200 units.")
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.NORMAL)
                .assignedTo(2L)
                .comments(List.of())
                .statusHistory(List.of())
                .build();
    }
}

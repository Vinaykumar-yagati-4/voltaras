package com.voltaras.complaintservice.controller;

import com.voltaras.complaintservice.dto.response.CategoryResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.ComplaintSummaryResponse;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.exception.AccessDeniedException;
import com.voltaras.complaintservice.exception.ResourceNotFoundException;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ComplaintController}.
 */
@WebMvcTest(ComplaintController.class)
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintService complaintService;

    // --------------------------------------------------------------
    // Create
    // --------------------------------------------------------------

    @Test
    @DisplayName("POST /api/complaints: valid creation returns 201 with Location")
    void createComplaint_returns201WithLocation() throws Exception {

        when(complaintService.createComplaint(eq(13L), eq("CONSUMER"), any()))
                .thenReturn(buildDetail());

        mockMvc.perform(post("/api/complaints")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "subject": "Incorrect bill amount for July 2026",
                                  "description": "My bill shows 350 units consumed but I only used about 200 units."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/complaints/5"))
                .andExpect(jsonPath("$.ticketNumber").value("CMP-20260812-0001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/complaints: invalid body returns 400 VALIDATION_ERROR")
    void createComplaint_validationFailure_returns400() throws Exception {

        mockMvc.perform(post("/api/complaints")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "subject": "short",
                                  "description": "too short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").exists());
    }

    @Test
    @DisplayName("POST /api/complaints: missing X-User-Id header returns 400 MISSING_HEADER")
    void createComplaint_missingHeader_returns400() throws Exception {

        mockMvc.perform(post("/api/complaints")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "subject": "Incorrect bill amount for July 2026",
                                  "description": "My bill shows 350 units consumed but I only used about 200 units."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    // --------------------------------------------------------------
    // List / detail
    // --------------------------------------------------------------

    @Test
    @DisplayName("GET /api/complaints: returns the caller's paginated complaints")
    void getMyComplaints_returns200() throws Exception {

        Page<ComplaintSummaryResponse> page = new PageImpl<>(
                List.of(buildSummary()),
                PageRequest.of(0, 10),
                1);

        when(complaintService.getMyComplaints(eq(13L), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/complaints")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber")
                        .value("CMP-20260812-0001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/complaints/{id}: foreign complaint returns 404 RESOURCE_NOT_FOUND")
    void getMyComplaint_foreign_returns404() throws Exception {

        when(complaintService.getMyComplaint(13L, 99L))
                .thenThrow(new ResourceNotFoundException("Complaint", "id", 99L));

        mockMvc.perform(get("/api/complaints/99")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/complaints/ticket/{ticketNumber}: returns the owned complaint")
    void getMyComplaintByTicket_returns200() throws Exception {

        when(complaintService.getMyComplaintByTicketNumber(
                13L, "CMP-20260812-0001"))
                .thenReturn(buildDetail());

        mockMvc.perform(get("/api/complaints/ticket/CMP-20260812-0001")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("CMP-20260812-0001"));
    }

    // --------------------------------------------------------------
    // Edit / comments
    // --------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/complaints/{id}: owner edit while OPEN returns 200")
    void updateMyComplaint_returns200() throws Exception {

        when(complaintService.updateMyComplaint(eq(13L), eq(5L), any()))
                .thenReturn(buildDetail());

        mockMvc.perform(put("/api/complaints/5")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Updated subject line",
                                  "description": "Updated detailed description text."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/comments: returns 201")
    void addConsumerComment_returns201() throws Exception {

        mockMvc.perform(post("/api/complaints/5/comments")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentText": "Please review the meter reading."}
                                """))
                .andExpect(status().isCreated());
    }

    // --------------------------------------------------------------
    // Lookups
    // --------------------------------------------------------------

    @Test
    @DisplayName("GET /api/complaints/categories: returns active categories")
    void getCategories_returns200() throws Exception {

        when(complaintService.getActiveCategories())
                .thenReturn(List.of(
                        CategoryResponse.builder()
                                .id(1L)
                                .name("BILLING_ISSUE")
                                .description("Issues with the computed electricity bill")
                                .build()));

        mockMvc.perform(get("/api/complaints/categories")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("BILLING_ISSUE"));
    }

    @Test
    @DisplayName("GET /api/complaints/internal/count: ADMIN returns per-status counts")
    void getStatusCounts_admin_returns200() throws Exception {

        Map<ComplaintStatus, Long> counts = new EnumMap<>(ComplaintStatus.class);
        counts.put(ComplaintStatus.OPEN, 3L);

        when(complaintService.getStatusCounts("ADMIN")).thenReturn(counts);

        mockMvc.perform(get("/api/complaints/internal/count")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OPEN").value(3));
    }

    @Test
    @DisplayName("GET /api/complaints/internal/count: CONSUMER role returns 403")
    void getStatusCounts_consumer_returns403() throws Exception {

        when(complaintService.getStatusCounts("CONSUMER"))
                .thenThrow(new AccessDeniedException(
                        "Only ADMIN users can perform this operation"));

        mockMvc.perform(get("/api/complaints/internal/count")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/complaints/{id}: service AccessDenied maps to 403")
    void getMyComplaint_denied_returns403() throws Exception {

        when(complaintService.getMyComplaint(13L, 5L))
                .thenThrow(new AccessDeniedException("You are not allowed to access this complaint"));

        mockMvc.perform(get("/api/complaints/5")
                        .header("X-User-Id", "13")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // --------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------

    private ComplaintSummaryResponse buildSummary() {

        return ComplaintSummaryResponse.builder()
                .id(5L)
                .ticketNumber("CMP-20260812-0001")
                .consumerId(13L)
                .categoryId(1L)
                .categoryName("BILLING_ISSUE")
                .subject("Incorrect bill amount for July 2026")
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
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
                .comments(List.of())
                .statusHistory(List.of())
                .build();
    }
}

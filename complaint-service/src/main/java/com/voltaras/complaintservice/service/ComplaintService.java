package com.voltaras.complaintservice.service;

import com.voltaras.complaintservice.dto.request.AddComplaintCommentRequest;
import com.voltaras.complaintservice.dto.request.AssignComplaintRequest;
import com.voltaras.complaintservice.dto.request.CreateComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintStatusRequest;
import com.voltaras.complaintservice.dto.response.CategoryResponse;
import com.voltaras.complaintservice.dto.response.CommentResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.ComplaintSummaryResponse;
import com.voltaras.complaintservice.dto.response.StatusUpdateResponse;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Complaint business operations. Identity and role come from the
 * gateway-injected {@code X-User-Id} / {@code X-User-Role} headers.
 */
public interface ComplaintService {

    // --------------------------------------------------------------
    // Consumer APIs
    // --------------------------------------------------------------

    ComplaintDetailResponse createComplaint(
            Long authUserId, String systemRole, CreateComplaintRequest request);

    Page<ComplaintSummaryResponse> getMyComplaints(
            Long authUserId,
            ComplaintStatus status,
            ComplaintPriority priority,
            Long categoryId,
            Pageable pageable);

    ComplaintDetailResponse getMyComplaint(Long authUserId, Long complaintId);

    ComplaintDetailResponse getMyComplaintByTicketNumber(
            Long authUserId, String ticketNumber);

    ComplaintDetailResponse updateMyComplaint(
            Long authUserId, Long complaintId, UpdateComplaintRequest request);

    CommentResponse addConsumerComment(
            Long authUserId, Long complaintId, AddComplaintCommentRequest request);

    List<CategoryResponse> getActiveCategories();

    // --------------------------------------------------------------
    // Admin APIs
    // --------------------------------------------------------------

    Page<ComplaintSummaryResponse> getAllComplaintsForAdmin(
            String systemRole,
            ComplaintStatus status,
            ComplaintPriority priority,
            Long categoryId,
            Long consumerId,
            Long assignedTo,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    ComplaintDetailResponse getComplaintForAdmin(String systemRole, Long complaintId);

    ComplaintDetailResponse getComplaintByTicketNumberForAdmin(
            String systemRole, String ticketNumber);

    StatusUpdateResponse updateComplaintStatus(
            String systemRole, Long adminUserId, Long complaintId,
            UpdateComplaintStatusRequest request);

    ComplaintDetailResponse assignComplaint(
            String systemRole, Long adminUserId, Long complaintId,
            AssignComplaintRequest request);

    CommentResponse addAdminComment(
            String systemRole, Long adminUserId, Long complaintId,
            AddComplaintCommentRequest request);

    // --------------------------------------------------------------
    // Internal APIs
    // --------------------------------------------------------------

    /**
     * Per-status complaint counts for the admin dashboard. Requires the
     * system {@code ADMIN} role (the Dashboard Service does not exist yet;
     * until then the endpoint is restricted to the existing admin
     * convention instead of every authenticated user).
     */
    Map<ComplaintStatus, Long> getStatusCounts(String systemRole);
}

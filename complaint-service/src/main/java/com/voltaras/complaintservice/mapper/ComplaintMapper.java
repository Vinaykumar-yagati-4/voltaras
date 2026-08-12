package com.voltaras.complaintservice.mapper;

import com.voltaras.complaintservice.dto.response.CommentResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.ComplaintSummaryResponse;
import com.voltaras.complaintservice.dto.response.StatusHistoryResponse;
import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.entity.ComplaintComment;
import com.voltaras.complaintservice.entity.ComplaintStatusHistory;

import java.util.List;

/**
 * Manual conversions between {@link Complaint} entities and response DTOs
 * (VOLTARAS manual-mapper convention).
 */
public final class ComplaintMapper {

    private ComplaintMapper() {
        // Prevent object creation for utility class.
    }

    /**
     * Converts an entity into the lightweight list response DTO.
     */
    public static ComplaintSummaryResponse toSummaryResponse(Complaint complaint) {

        return ComplaintSummaryResponse.builder()
                .id(complaint.getId())
                .ticketNumber(complaint.getTicketNumber())
                .consumerId(complaint.getConsumerId())
                .categoryId(complaint.getCategory().getId())
                .categoryName(complaint.getCategory().getName())
                .subject(complaint.getSubject())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .assignedTo(complaint.getAssignedTo())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    /**
     * Converts an entity into the full response DTO including comments and
     * status history.
     */
    public static ComplaintDetailResponse toDetailResponse(Complaint complaint) {

        List<CommentResponse> comments = complaint.getComments() == null
                ? List.of()
                : complaint.getComments().stream()
                        .map(ComplaintMapper::toCommentResponse)
                        .toList();

        List<StatusHistoryResponse> history = complaint.getStatusHistory() == null
                ? List.of()
                : complaint.getStatusHistory().stream()
                        .map(ComplaintMapper::toHistoryResponse)
                        .toList();

        return ComplaintDetailResponse.builder()
                .id(complaint.getId())
                .ticketNumber(complaint.getTicketNumber())
                .consumerId(complaint.getConsumerId())
                .categoryId(complaint.getCategory().getId())
                .categoryName(complaint.getCategory().getName())
                .subject(complaint.getSubject())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .assignedTo(complaint.getAssignedTo())
                .resolvedAt(complaint.getResolvedAt())
                .closedAt(complaint.getClosedAt())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .comments(comments)
                .statusHistory(history)
                .build();
    }

    /**
     * Converts a comment entity into its response DTO.
     */
    public static CommentResponse toCommentResponse(ComplaintComment comment) {

        return CommentResponse.builder()
                .id(comment.getId())
                .commentText(comment.getCommentText())
                .authorId(comment.getAuthorId())
                .isAdminComment(comment.isAdminComment())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * Converts a status-history entity into its response DTO.
     */
    public static StatusHistoryResponse toHistoryResponse(
            ComplaintStatusHistory history) {

        return StatusHistoryResponse.builder()
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }
}

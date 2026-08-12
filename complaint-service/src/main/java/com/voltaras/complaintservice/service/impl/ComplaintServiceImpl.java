package com.voltaras.complaintservice.service.impl;

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
import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.entity.ComplaintCategory;
import com.voltaras.complaintservice.entity.ComplaintComment;
import com.voltaras.complaintservice.entity.ComplaintStatusHistory;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.exception.BadRequestException;
import com.voltaras.complaintservice.exception.ResourceNotFoundException;
import com.voltaras.complaintservice.mapper.ComplaintCategoryMapper;
import com.voltaras.complaintservice.mapper.ComplaintMapper;
import com.voltaras.complaintservice.messaging.ComplaintEventPublisher;
import com.voltaras.complaintservice.repository.ComplaintCategoryRepository;
import com.voltaras.complaintservice.repository.ComplaintCommentRepository;
import com.voltaras.complaintservice.repository.ComplaintRepository;
import com.voltaras.complaintservice.security.ComplaintAccessHelper;
import com.voltaras.complaintservice.service.ComplaintService;
import com.voltaras.complaintservice.util.ComplaintStatusTransitions;
import com.voltaras.complaintservice.util.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ComplaintService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintCommentRepository commentRepository;
    private final ComplaintCategoryRepository categoryRepository;
    private final ComplaintAccessHelper accessHelper;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final ComplaintEventPublisher eventPublisher;

    // --------------------------------------------------------------
    // Consumer APIs
    // --------------------------------------------------------------

    @Override
    @Transactional
    public ComplaintDetailResponse createComplaint(
            Long authUserId, String systemRole, CreateComplaintRequest request) {

        accessHelper.requireConsumer(systemRole);

        ComplaintCategory category = categoryRepository
                .findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException(
                        "Complaint category is not active or does not exist"));

        Complaint complaint = Complaint.builder()
                .ticketNumber(ticketNumberGenerator.generateNextTicketNumber())
                .consumerId(authUserId)
                .category(category)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.NORMAL)
                .build();

        // Initial audit entry (OPEN), matching the status-history table design.
        complaint.getStatusHistory().add(ComplaintStatusHistory.builder()
                .complaint(complaint)
                .fromStatus(null)
                .toStatus(ComplaintStatus.OPEN.name())
                .changedBy(authUserId)
                .build());

        // saveAndFlush: populates the generated id and the initial status-
        // history changedAt before the response is mapped.
        Complaint saved = complaintRepository.saveAndFlush(complaint);

        log.info("Complaint created: id={}, ticketNumber={}, consumerId={}",
                saved.getId(), saved.getTicketNumber(), saved.getConsumerId());

        return ComplaintMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getMyComplaints(
            Long authUserId,
            ComplaintStatus status,
            ComplaintPriority priority,
            Long categoryId,
            Pageable pageable) {

        return complaintRepository
                .searchForConsumer(authUserId, status, priority, categoryId, pageable)
                .map(ComplaintMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getMyComplaint(Long authUserId, Long complaintId) {

        Complaint complaint = complaintRepository
                .findByIdAndConsumerId(complaintId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "id", complaintId));

        return ComplaintMapper.toDetailResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getMyComplaintByTicketNumber(
            Long authUserId, String ticketNumber) {

        Complaint complaint = complaintRepository
                .findByTicketNumberAndConsumerId(ticketNumber, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "ticketNumber", ticketNumber));

        return ComplaintMapper.toDetailResponse(complaint);
    }

    @Override
    @Transactional
    public ComplaintDetailResponse updateMyComplaint(
            Long authUserId, Long complaintId, UpdateComplaintRequest request) {

        Complaint complaint = complaintRepository
                .findByIdAndConsumerId(complaintId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "id", complaintId));

        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new BadRequestException(
                    "Complaints can only be edited while OPEN");
        }

        complaint.setSubject(request.getSubject().trim());
        complaint.setDescription(request.getDescription().trim());

        // saveAndFlush: @PreUpdate must run before the response maps updatedAt.
        Complaint saved = complaintRepository.saveAndFlush(complaint);

        log.info("Complaint {} updated by owner {}", saved.getId(), authUserId);

        return ComplaintMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse addConsumerComment(
            Long authUserId, Long complaintId, AddComplaintCommentRequest request) {

        Complaint complaint = complaintRepository
                .findByIdAndConsumerId(complaintId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "id", complaintId));

        ensureCommentsAllowed(complaint);

        ComplaintComment comment = ComplaintComment.builder()
                .complaint(complaint)
                .commentText(request.getCommentText().trim())
                .authorId(authUserId)
                .isAdminComment(false)
                .build();

        // Save directly: with IDENTITY generation the insert runs immediately
        // so the response carries the generated id and createdAt timestamp.
        ComplaintComment savedComment = commentRepository.save(comment);

        complaint.getComments().add(savedComment);

        log.info("Consumer comment added to complaint {} by user {}",
                complaintId, authUserId);

        return ComplaintMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {

        return categoryRepository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(ComplaintCategoryMapper::toResponse)
                .toList();
    }

    // --------------------------------------------------------------
    // Admin APIs
    // --------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getAllComplaintsForAdmin(
            String systemRole,
            ComplaintStatus status,
            ComplaintPriority priority,
            Long categoryId,
            Long consumerId,
            Long assignedTo,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        accessHelper.requireAdmin(systemRole);

        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);

        return complaintRepository
                .searchForAdmin(status, priority, categoryId, consumerId,
                        assignedTo, from, to, pageable)
                .map(ComplaintMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintForAdmin(
            String systemRole, Long complaintId) {

        accessHelper.requireAdmin(systemRole);

        Complaint complaint = findComplaintOrThrow(complaintId);

        return ComplaintMapper.toDetailResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintByTicketNumberForAdmin(
            String systemRole, String ticketNumber) {

        accessHelper.requireAdmin(systemRole);

        Complaint complaint = complaintRepository
                .findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "ticketNumber", ticketNumber));

        return ComplaintMapper.toDetailResponse(complaint);
    }

    @Override
    @Transactional
    public StatusUpdateResponse updateComplaintStatus(
            String systemRole, Long adminUserId, Long complaintId,
            UpdateComplaintStatusRequest request) {

        accessHelper.requireAdmin(systemRole);

        Complaint complaint = findComplaintOrThrow(complaintId);

        ComplaintStatus current = complaint.getStatus();
        ComplaintStatus target = request.getStatus();

        if (current == target) {
            throw new BadRequestException(
                    "Complaint is already " + current);
        }

        if (!ComplaintStatusTransitions.canTransition(current, target)) {
            throw new BadRequestException(
                    "Invalid status transition from " + current + " to " + target
                            + ". Allowed transitions from " + current + ": "
                            + ComplaintStatusTransitions.allowedTargets(current));
        }

        complaint.setStatus(target);

        if (target == ComplaintStatus.RESOLVED && complaint.getResolvedAt() == null) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        if (target == ComplaintStatus.CLOSED && complaint.getClosedAt() == null) {
            complaint.setClosedAt(LocalDateTime.now());
        }

        // Audit trail entry.
        complaint.getStatusHistory().add(ComplaintStatusHistory.builder()
                .complaint(complaint)
                .fromStatus(current.name())
                .toStatus(target.name())
                .changedBy(adminUserId)
                .build());

        // saveAndFlush: @PreUpdate must run before the response maps updatedAt
        // and the status-history row is persisted before the event is sent.
        Complaint saved = complaintRepository.saveAndFlush(complaint);

        log.info("Complaint {} status changed {} -> {} by admin {}",
                saved.getId(), current, target, adminUserId);

        // Asynchronous notification (best-effort, never rolls back the update).
        eventPublisher.publishStatusChanged(saved, target);

        return StatusUpdateResponse.builder()
                .complaintId(saved.getId())
                .ticketNumber(saved.getTicketNumber())
                .previousStatus(current)
                .currentStatus(target)
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ComplaintDetailResponse assignComplaint(
            String systemRole, Long adminUserId, Long complaintId,
            AssignComplaintRequest request) {

        accessHelper.requireAdmin(systemRole);

        Complaint complaint = findComplaintOrThrow(complaintId);

        if (complaint.getStatus() != ComplaintStatus.OPEN
                && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {

            throw new BadRequestException(
                    "Complaints can only be assigned while OPEN or IN_PROGRESS");
        }

        complaint.setAssignedTo(request.getAssignedTo());

        // saveAndFlush: @PreUpdate must run before the response maps updatedAt.
        Complaint saved = complaintRepository.saveAndFlush(complaint);

        log.info("Complaint {} assigned to admin {} by admin {}",
                saved.getId(), saved.getAssignedTo(), adminUserId);

        return ComplaintMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse addAdminComment(
            String systemRole, Long adminUserId, Long complaintId,
            AddComplaintCommentRequest request) {

        accessHelper.requireAdmin(systemRole);

        Complaint complaint = findComplaintOrThrow(complaintId);

        ensureCommentsAllowed(complaint);

        ComplaintComment comment = ComplaintComment.builder()
                .complaint(complaint)
                .commentText(request.getCommentText().trim())
                .authorId(adminUserId)
                .isAdminComment(true)
                .build();

        // Save directly: with IDENTITY generation the insert runs immediately
        // so the response carries the generated id and createdAt timestamp.
        ComplaintComment savedComment = commentRepository.save(comment);

        complaint.getComments().add(savedComment);

        log.info("Admin comment added to complaint {} by admin {}",
                complaintId, adminUserId);

        return ComplaintMapper.toCommentResponse(savedComment);
    }

    // --------------------------------------------------------------
    // Internal APIs
    // --------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<ComplaintStatus, Long> getStatusCounts(String systemRole) {

        accessHelper.requireAdmin(systemRole);

        Map<ComplaintStatus, Long> counts = new EnumMap<>(ComplaintStatus.class);

        for (ComplaintStatus status : ComplaintStatus.values()) {
            counts.put(status, 0L);
        }

        complaintRepository.countGroupedByStatus().forEach(row -> {
            ComplaintStatus status = (ComplaintStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
        });

        return counts;
    }

    // --------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------

    private Complaint findComplaintOrThrow(Long complaintId) {

        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complaint", "id", complaintId));
    }

    private void ensureCommentsAllowed(Complaint complaint) {

        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BadRequestException(
                    "Cannot add a comment to a CLOSED complaint");
        }
    }
}

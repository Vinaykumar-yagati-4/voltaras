package com.voltaras.complaintservice.service;

import com.voltaras.complaintservice.dto.request.AddComplaintCommentRequest;
import com.voltaras.complaintservice.dto.request.AssignComplaintRequest;
import com.voltaras.complaintservice.dto.request.CreateComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintRequest;
import com.voltaras.complaintservice.dto.request.UpdateComplaintStatusRequest;
import com.voltaras.complaintservice.dto.response.CommentResponse;
import com.voltaras.complaintservice.dto.response.ComplaintDetailResponse;
import com.voltaras.complaintservice.dto.response.StatusUpdateResponse;
import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.entity.ComplaintCategory;
import com.voltaras.complaintservice.entity.ComplaintComment;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.exception.AccessDeniedException;
import com.voltaras.complaintservice.exception.BadRequestException;
import com.voltaras.complaintservice.exception.ResourceNotFoundException;
import com.voltaras.complaintservice.messaging.ComplaintEventPublisher;
import com.voltaras.complaintservice.repository.ComplaintCategoryRepository;
import com.voltaras.complaintservice.repository.ComplaintCommentRepository;
import com.voltaras.complaintservice.repository.ComplaintRepository;
import com.voltaras.complaintservice.security.ComplaintAccessHelper;
import com.voltaras.complaintservice.service.impl.ComplaintServiceImpl;
import com.voltaras.complaintservice.util.TicketNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ComplaintServiceImpl}: lifecycle transitions,
 * authorization boundaries, ownership and event publishing.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    private static final Long CONSUMER_ID = 13L;
    private static final Long ADMIN_ID = 1L;
    private static final Long COMPLAINT_ID = 5L;
    private static final String TICKET = "CMP-20260812-0001";

    @Mock private ComplaintRepository complaintRepository;
    @Mock private ComplaintCommentRepository commentRepository;
    @Mock private ComplaintCategoryRepository categoryRepository;
    @Mock private TicketNumberGenerator ticketNumberGenerator;
    @Mock private ComplaintEventPublisher eventPublisher;

    private ComplaintServiceImpl complaintService;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintServiceImpl(
                complaintRepository,
                commentRepository,
                categoryRepository,
                new ComplaintAccessHelper(),
                ticketNumberGenerator,
                eventPublisher);
    }

    // --------------------------------------------------------------
    // Create
    // --------------------------------------------------------------

    @Test
    @DisplayName("Create: consumer creates an OPEN/NORMAL complaint with a ticket number")
    void createComplaint_success() {

        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn(TICKET);
        when(categoryRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(buildCategory()));

        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> {
                    Complaint complaint = invocation.getArgument(0);
                    complaint.setId(COMPLAINT_ID);
                    return complaint;
                });

        ComplaintDetailResponse response = complaintService.createComplaint(
                CONSUMER_ID, "CONSUMER", buildCreateRequest());

        assertThat(response.getId()).isEqualTo(COMPLAINT_ID);
        assertThat(response.getTicketNumber()).isEqualTo(TICKET);
        assertThat(response.getStatus()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(ComplaintPriority.NORMAL);
        assertThat(response.getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(response.getStatusHistory()).hasSize(1);
        assertThat(response.getStatusHistory().getFirst().getToStatus())
                .isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Create: ADMIN role is rejected (consumer-only endpoint)")
    void createComplaint_adminRole_throwsAccessDenied() {

        assertThatThrownBy(() -> complaintService.createComplaint(
                ADMIN_ID, "ADMIN", buildCreateRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("CONSUMER");
    }

    @Test
    @DisplayName("Create: inactive/unknown category is rejected")
    void createComplaint_inactiveCategory_throwsBadRequest() {

        when(categoryRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        CreateComplaintRequest request = buildCreateRequest();
        request.setCategoryId(99L);

        assertThatThrownBy(() -> complaintService.createComplaint(
                CONSUMER_ID, "CONSUMER", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("category");

        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    // --------------------------------------------------------------
    // Ownership scoping
    // --------------------------------------------------------------

    @Test
    @DisplayName("Get my complaint: foreign complaint is not found")
    void getMyComplaint_foreignComplaint_throwsResourceNotFound() {

        when(complaintRepository.findByIdAndConsumerId(COMPLAINT_ID, CONSUMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.getMyComplaint(CONSUMER_ID, COMPLAINT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get my complaint by ticket: ownership scoped")
    void getMyComplaintByTicket_scopedToOwner() {

        when(complaintRepository.findByTicketNumberAndConsumerId(TICKET, CONSUMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.getMyComplaintByTicketNumber(CONSUMER_ID, TICKET))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --------------------------------------------------------------
    // Consumer edit
    // --------------------------------------------------------------

    @Test
    @DisplayName("Edit: allowed while OPEN")
    void updateMyComplaint_open_success() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);

        when(complaintRepository.findByIdAndConsumerId(COMPLAINT_ID, CONSUMER_ID))
                .thenReturn(Optional.of(complaint));
        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintDetailResponse response = complaintService.updateMyComplaint(
                CONSUMER_ID, COMPLAINT_ID,
                UpdateComplaintRequest.builder()
                        .subject("Updated subject line")
                        .description("Updated detailed description text.")
                        .build());

        assertThat(response.getSubject()).isEqualTo("Updated subject line");
    }

    @Test
    @DisplayName("Edit: blocked when no longer OPEN")
    void updateMyComplaint_notOpen_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.IN_PROGRESS);

        when(complaintRepository.findByIdAndConsumerId(COMPLAINT_ID, CONSUMER_ID))
                .thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.updateMyComplaint(
                CONSUMER_ID, COMPLAINT_ID,
                UpdateComplaintRequest.builder()
                        .subject("Updated subject line")
                        .description("Updated detailed description text.")
                        .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OPEN");

        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    // --------------------------------------------------------------
    // Lifecycle transitions
    // --------------------------------------------------------------

    @Test
    @DisplayName("Status: OPEN -> IN_PROGRESS records history and publishes the event")
    void updateStatus_openToInProgress_success() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateResponse response = complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.IN_PROGRESS)
                        .build());

        assertThat(response.getPreviousStatus()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(response.getCurrentStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
        assertThat(complaint.getResolvedAt()).isNull();
        assertThat(complaint.getStatusHistory()).hasSize(1);
        assertThat(complaint.getStatusHistory().getFirst().getChangedBy())
                .isEqualTo(ADMIN_ID);

        verify(eventPublisher).publishStatusChanged(complaint, ComplaintStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Status: IN_PROGRESS -> RESOLVED sets resolvedAt")
    void updateStatus_resolved_setsResolvedAt() {

        Complaint complaint = buildComplaint(ComplaintStatus.IN_PROGRESS);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.RESOLVED)
                        .build());

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(complaint.getResolvedAt()).isNotNull();
        assertThat(complaint.getClosedAt()).isNull();

        verify(eventPublisher).publishStatusChanged(complaint, ComplaintStatus.RESOLVED);
    }

    @Test
    @DisplayName("Status: RESOLVED -> CLOSED sets closedAt")
    void updateStatus_closed_setsClosedAt() {

        Complaint complaint = buildComplaint(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now().minusDays(1));

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.CLOSED)
                        .build());

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.CLOSED);
        assertThat(complaint.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("Status: same-status transition is rejected")
    void updateStatus_sameStatus_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.OPEN)
                        .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already");

        verify(complaintRepository, never()).save(any(Complaint.class));
        verify(eventPublisher, never()).publishStatusChanged(any(), any());
    }

    @Test
    @DisplayName("Status: invalid transition OPEN -> CLOSED is rejected with allowed targets")
    void updateStatus_invalidTransition_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.CLOSED)
                        .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition")
                .hasMessageContaining("IN_PROGRESS");

        verify(eventPublisher, never()).publishStatusChanged(any(), any());
    }

    @Test
    @DisplayName("Status: CLOSED is terminal")
    void updateStatus_closedTerminal_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.CLOSED);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.updateComplaintStatus(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.OPEN)
                        .build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Status: CONSUMER role is rejected")
    void updateStatus_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() -> complaintService.updateComplaintStatus(
                "CONSUMER", CONSUMER_ID, COMPLAINT_ID,
                UpdateComplaintStatusRequest.builder()
                        .status(ComplaintStatus.IN_PROGRESS)
                        .build()))
                .isInstanceOf(AccessDeniedException.class);

        verify(complaintRepository, never()).findById(any());
    }

    // --------------------------------------------------------------
    // Assignment
    // --------------------------------------------------------------

    @Test
    @DisplayName("Assign: allowed while OPEN")
    void assignComplaint_open_success() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.saveAndFlush(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintDetailResponse response = complaintService.assignComplaint(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                AssignComplaintRequest.builder().assignedTo(2L).build());

        assertThat(response.getAssignedTo()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Assign: blocked for RESOLVED complaints")
    void assignComplaint_resolved_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.RESOLVED);

        when(complaintRepository.findById(COMPLAINT_ID)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.assignComplaint(
                "ADMIN", ADMIN_ID, COMPLAINT_ID,
                AssignComplaintRequest.builder().assignedTo(2L).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OPEN or IN_PROGRESS");
    }

    // --------------------------------------------------------------
    // Comments
    // --------------------------------------------------------------

    @Test
    @DisplayName("Consumer comment: success returns the saved comment with generated id")
    void addConsumerComment_success_returnsSavedComment() {

        Complaint complaint = buildComplaint(ComplaintStatus.OPEN);
        ComplaintComment comment = ComplaintComment.builder()
                .id(42L)
                .complaint(complaint)
                .commentText("Please check my reading.")
                .authorId(CONSUMER_ID)
                .isAdminComment(false)
                .build();

        when(complaintRepository.findByIdAndConsumerId(COMPLAINT_ID, CONSUMER_ID))
                .thenReturn(Optional.of(complaint));
        when(commentRepository.save(any(ComplaintComment.class)))
                .thenAnswer(invocation -> {
                    ComplaintComment saved = invocation.getArgument(0);
                    saved.setId(42L);
                    return saved;
                });

        CommentResponse response = complaintService.addConsumerComment(
                CONSUMER_ID, COMPLAINT_ID,
                AddComplaintCommentRequest.builder()
                        .commentText("Please check my reading.")
                        .build());

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getAuthorId()).isEqualTo(CONSUMER_ID);
        assertThat(response.isAdminComment()).isFalse();
        verify(commentRepository).save(any(ComplaintComment.class));
    }

    @Test
    @DisplayName("Assign: consumer role is rejected")
    void assignComplaint_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() -> complaintService.assignComplaint(
                "CONSUMER", CONSUMER_ID, COMPLAINT_ID,
                AssignComplaintRequest.builder().assignedTo(2L).build()))
                .isInstanceOf(AccessDeniedException.class);

        verify(complaintRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Consumer comment: rejected on CLOSED complaints")
    void addConsumerComment_closed_throwsBadRequest() {

        Complaint complaint = buildComplaint(ComplaintStatus.CLOSED);

        when(complaintRepository.findByIdAndConsumerId(COMPLAINT_ID, CONSUMER_ID))
                .thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.addConsumerComment(
                CONSUMER_ID, COMPLAINT_ID,
                AddComplaintCommentRequest.builder().commentText("Still an issue").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("Admin comment: consumer role is rejected")
    void addAdminComment_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() -> complaintService.addAdminComment(
                "CONSUMER", CONSUMER_ID, COMPLAINT_ID,
                AddComplaintCommentRequest.builder().commentText("Resolution note").build()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --------------------------------------------------------------
    // Counts
    // --------------------------------------------------------------

    @Test
    @DisplayName("Status counts: ADMIN sees all statuses with zero-filled gaps")
    void getStatusCounts_admin_zeroFillsMissingStatuses() {

        when(complaintRepository.countGroupedByStatus()).thenReturn(
                java.util.List.of(
                        new Object[]{ComplaintStatus.OPEN, 3L},
                        new Object[]{ComplaintStatus.IN_PROGRESS, 1L}
                ));

        Map<ComplaintStatus, Long> counts =
                complaintService.getStatusCounts("ADMIN");

        assertThat(counts.get(ComplaintStatus.OPEN)).isEqualTo(3L);
        assertThat(counts.get(ComplaintStatus.IN_PROGRESS)).isEqualTo(1L);
        assertThat(counts.get(ComplaintStatus.RESOLVED)).isEqualTo(0L);
        assertThat(counts.get(ComplaintStatus.CLOSED)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Status counts: non-admin role is rejected")
    void getStatusCounts_consumerRole_throwsAccessDenied() {

        assertThatThrownBy(() -> complaintService.getStatusCounts("CONSUMER"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------

    private CreateComplaintRequest buildCreateRequest() {

        return CreateComplaintRequest.builder()
                .categoryId(1L)
                .subject("Incorrect bill amount for July 2026")
                .description("My bill shows 350 units consumed but I only used about 200 units.")
                .build();
    }

    private ComplaintCategory buildCategory() {

        return ComplaintCategory.builder()
                .id(1L)
                .name("BILLING_ISSUE")
                .description("Issues with the computed electricity bill")
                .active(true)
                .build();
    }

    private Complaint buildComplaint(ComplaintStatus status) {

        return Complaint.builder()
                .id(COMPLAINT_ID)
                .ticketNumber(TICKET)
                .consumerId(CONSUMER_ID)
                .category(buildCategory())
                .subject("Incorrect bill amount for July 2026")
                .description("My bill shows 350 units consumed but I only used about 200 units.")
                .status(status)
                .priority(ComplaintPriority.NORMAL)
                .build();
    }
}

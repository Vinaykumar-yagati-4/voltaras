package com.voltaras.complaintservice.repository;

import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.entity.ComplaintCategory;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository-layer tests for {@link ComplaintRepository} running against
 * the in-memory H2 database.
 */
@DataJpaTest
class ComplaintRepositoryTest {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintCategoryRepository categoryRepository;

    private ComplaintCategory billing;
    private ComplaintCategory meter;

    @BeforeEach
    void setUp() {

        billing = categoryRepository.save(ComplaintCategory.builder()
                .name("BILLING_ISSUE")
                .description("Issues with the computed electricity bill")
                .active(true)
                .build());

        meter = categoryRepository.save(ComplaintCategory.builder()
                .name("METER_ISSUE")
                .description("Issues with the meter")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("findByIdAndConsumerId scopes by owner")
    void findByIdAndConsumerId_scopesByOwner() {

        Complaint owned = complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));

        complaintRepository.save(
                buildComplaint("CMP-20260812-0002", 99L, billing, ComplaintStatus.OPEN));

        Optional<Complaint> found =
                complaintRepository.findByIdAndConsumerId(owned.getId(), 13L);

        assertThat(found).isPresent();
        assertThat(found.get().getTicketNumber()).isEqualTo("CMP-20260812-0001");

        assertThat(complaintRepository.findByIdAndConsumerId(owned.getId(), 999L))
                .isEmpty();
    }

    @Test
    @DisplayName("Ticket lookups work for owner-scoped and global access")
    void ticketNumberLookups() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));

        assertThat(complaintRepository.findByTicketNumber("CMP-20260812-0001"))
                .isPresent();

        assertThat(complaintRepository
                .findByTicketNumberAndConsumerId("CMP-20260812-0001", 13L))
                .isPresent();

        assertThat(complaintRepository
                .findByTicketNumberAndConsumerId("CMP-20260812-0001", 99L))
                .isEmpty();
    }

    @Test
    @DisplayName("countByTicketNumberStartingWith counts only the matching prefix")
    void countByTicketPrefix() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0002", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260813-0001", 13L, billing, ComplaintStatus.OPEN));

        assertThat(complaintRepository.countByTicketNumberStartingWith("CMP-20260812-"))
                .isEqualTo(2L);
        assertThat(complaintRepository.countByTicketNumberStartingWith("CMP-20260813-"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("searchForConsumer applies owner scope and optional filters")
    void searchForConsumer_filters() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0002", 13L, meter, ComplaintStatus.IN_PROGRESS));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0003", 99L, billing, ComplaintStatus.OPEN));

        PageRequest pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // All of the caller's complaints.
        Page<Complaint> all = complaintRepository
                .searchForConsumer(13L, null, null, null, pageable);
        assertThat(all.getContent()).hasSize(2);

        // Status filter.
        Page<Complaint> open = complaintRepository
                .searchForConsumer(13L, ComplaintStatus.OPEN, null, null, pageable);
        assertThat(open.getContent()).hasSize(1);
        assertThat(open.getContent().getFirst().getTicketNumber())
                .isEqualTo("CMP-20260812-0001");

        // Category filter.
        Page<Complaint> byCategory = complaintRepository
                .searchForConsumer(13L, null, null, meter.getId(), pageable);
        assertThat(byCategory.getContent()).hasSize(1);
        assertThat(byCategory.getContent().getFirst().getCategory().getName())
                .isEqualTo("METER_ISSUE");

        // Priority filter (all default NORMAL).
        Page<Complaint> byPriority = complaintRepository
                .searchForConsumer(13L, null, ComplaintPriority.NORMAL, null, pageable);
        assertThat(byPriority.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("searchForAdmin combines optional filters including dates")
    void searchForAdmin_filters() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0002", 13L, meter, ComplaintStatus.IN_PROGRESS));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0003", 99L, billing, ComplaintStatus.OPEN));

        PageRequest pageable = PageRequest.of(0, 10);

        // No filters -> all.
        assertThat(complaintRepository
                .searchForAdmin(null, null, null, null, null, null, null, pageable)
                .getContent()).hasSize(3);

        // Consumer filter.
        assertThat(complaintRepository
                .searchForAdmin(null, null, null, 13L, null, null, null, pageable)
                .getContent()).hasSize(2);

        // Status + assignee filter (nothing assigned -> 0).
        assertThat(complaintRepository
                .searchForAdmin(ComplaintStatus.OPEN, null, null, null, 2L, null, null, pageable)
                .getContent()).isEmpty();

        // Date window covering today.
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        assertThat(complaintRepository
                .searchForAdmin(null, null, null, null, null, from, to, pageable)
                .getContent()).hasSize(3);
    }

    @Test
    @DisplayName("countGroupedByStatus returns per-status counts")
    void countGroupedByStatus() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0002", 13L, billing, ComplaintStatus.OPEN));
        complaintRepository.save(
                buildComplaint("CMP-20260812-0003", 13L, billing, ComplaintStatus.IN_PROGRESS));

        Map<ComplaintStatus, Long> counts = complaintRepository
                .countGroupedByStatus()
                .stream()
                .collect(Collectors.toMap(
                        row -> (ComplaintStatus) row[0],
                        row -> ((Number) row[1]).longValue()));

        assertThat(counts.get(ComplaintStatus.OPEN)).isEqualTo(2L);
        assertThat(counts.get(ComplaintStatus.IN_PROGRESS)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Duplicate ticket numbers violate the unique constraint")
    void duplicateTicketNumber_violatesUniqueConstraint() {

        complaintRepository.save(
                buildComplaint("CMP-20260812-0001", 13L, billing, ComplaintStatus.OPEN));

        assertThatThrownBy(() -> complaintRepository.saveAndFlush(
                buildComplaint("CMP-20260812-0001", 99L, billing, ComplaintStatus.OPEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Complaint buildComplaint(
            String ticketNumber, Long consumerId, ComplaintCategory category,
            ComplaintStatus status) {

        return Complaint.builder()
                .ticketNumber(ticketNumber)
                .consumerId(consumerId)
                .category(category)
                .subject("Incorrect bill amount for July 2026")
                .description("My bill shows 350 units consumed but I only used about 200 units.")
                .status(status)
                .priority(ComplaintPriority.NORMAL)
                .build();
    }
}

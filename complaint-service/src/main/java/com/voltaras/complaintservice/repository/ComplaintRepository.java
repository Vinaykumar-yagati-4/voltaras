package com.voltaras.complaintservice.repository;

import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /**
     * Ownership-scoped lookup: only returns the complaint when it belongs
     * to the given consumer.
     */
    Optional<Complaint> findByIdAndConsumerId(Long id, Long consumerId);

    Optional<Complaint> findByTicketNumber(String ticketNumber);

    /**
     * Ownership-scoped ticket lookup.
     */
    Optional<Complaint> findByTicketNumberAndConsumerId(String ticketNumber, Long consumerId);

    /**
     * Used by the ticket-number generator to derive the per-day sequence.
     */
    long countByTicketNumberStartingWith(String prefix);

    /**
     * Consumer's own complaints with optional filters. Null filter values
     * are ignored.
     */
    @Query("""
            SELECT c FROM Complaint c
            WHERE c.consumerId = :consumerId
              AND (:status IS NULL OR c.status = :status)
              AND (:priority IS NULL OR c.priority = :priority)
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
            """)
    Page<Complaint> searchForConsumer(
            @Param("consumerId") Long consumerId,
            @Param("status") ComplaintStatus status,
            @Param("priority") ComplaintPriority priority,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    /**
     * Admin listing with optional filters. Null filter values are ignored.
     */
    @Query("""
            SELECT c FROM Complaint c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:priority IS NULL OR c.priority = :priority)
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:consumerId IS NULL OR c.consumerId = :consumerId)
              AND (:assignedTo IS NULL OR c.assignedTo = :assignedTo)
              AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
              AND (:toDate IS NULL OR c.createdAt <= :toDate)
            """)
    Page<Complaint> searchForAdmin(
            @Param("status") ComplaintStatus status,
            @Param("priority") ComplaintPriority priority,
            @Param("categoryId") Long categoryId,
            @Param("consumerId") Long consumerId,
            @Param("assignedTo") Long assignedTo,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    /**
     * Per-status counts (dashboard KPI).
     */
    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countGroupedByStatus();
}

package com.voltaras.complaintservice.entity;

import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A consumer complaint.
 *
 * <p>
 * {@code consumerId} and {@code assignedTo} are external references: the
 * consumer is the Auth Service user ID received from {@code X-User-Id} and
 * the assignee is an Auth Service admin user ID. No foreign keys exist
 * across microservice databases.
 * </p>
 */
@Entity
@Table(
        name = "complaints",
        indexes = {
                @Index(name = "idx_complaints_consumer_id", columnList = "consumer_id"),
                @Index(name = "idx_complaints_status", columnList = "status"),
                @Index(name = "idx_complaints_priority", columnList = "priority"),
                @Index(name = "idx_complaints_category_id", columnList = "category_id"),
                @Index(name = "idx_complaints_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_complaints_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable, unique, human-readable ticket number (e.g.
     * {@code CMP-20260812-0001}), generated at creation.
     */
    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    /**
     * Auth Service user ID of the complaint owner, received from the
     * gateway-injected {@code X-User-Id} header. External reference to
     * {@code auth_db.users.id}; no database foreign key.
     */
    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ComplaintCategory category;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ComplaintPriority priority;

    /**
     * Auth Service admin user ID the complaint is assigned to, or null
     * when unassigned. External reference to {@code auth_db.users.id}.
     */
    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(
            mappedBy = "complaint",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt ASC")
    private List<ComplaintComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "complaint",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("changedAt ASC")
    private List<ComplaintStatusHistory> statusHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ComplaintStatus.OPEN;
        }

        if (priority == null) {
            priority = ComplaintPriority.NORMAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

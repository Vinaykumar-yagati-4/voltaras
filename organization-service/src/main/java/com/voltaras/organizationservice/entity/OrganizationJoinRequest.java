package com.voltaras.organizationservice.entity;

import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.enums.MembershipRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_join_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_join_request_organization")
    )
    private Organization organization;

    /**
     * External Auth Service user ID (from X-User-Id via the API Gateway).
     */
    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 30)
    private MembershipRole requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JoinRequestStatus status;

    @Column(name = "request_message", length = 500)
    private String requestMessage;

    @Column(name = "rejection_remarks", length = 500)
    private String rejectionRemarks;

    /**
     * Reviewer (OWNER or ORGANIZATION_ADMIN) who approved or rejected.
     */
    @Column(name = "reviewed_by_auth_user_id")
    private Long reviewedByAuthUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = JoinRequestStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

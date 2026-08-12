package com.voltaras.complaintservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A comment on a complaint, added either by the complaint owner
 * ({@code isAdminComment = false}) or by an admin
 * ({@code isAdminComment = true}).
 */
@Entity
@Table(
        name = "complaint_comments",
        indexes = @Index(
                name = "idx_complaint_comments_complaint_id",
                columnList = "complaint_id"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    /**
     * Auth Service user ID of the comment author (consumer or admin).
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "is_admin_comment", nullable = false)
    private boolean isAdminComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
    }
}

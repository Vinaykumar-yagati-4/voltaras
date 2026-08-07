package com.voltaras.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Server-side record of a single issued refresh token.
 *
 * Only the SHA-256 hash of the raw refresh token is ever stored - the
 * raw token travels exclusively inside the JWT handed to the client.
 * Every refresh token created by a login or a rotation belongs to a
 * secure, random session identifier shared with the access token of
 * the same login, so logout can revoke the whole session.
 */
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refresh_tokens_token_hash",
                columnNames = "token_hash"
        ),
        indexes = {
                @Index(
                        name = "idx_refresh_tokens_session_id",
                        columnList = "session_id"
                ),
                @Index(
                        name = "idx_refresh_tokens_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_refresh_tokens_user_active",
                        columnList = "user_id, revoked_at"
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    /**
     * Secure, random session identifier shared by the access token
     * (sid claim) and every refresh token issued for that login.
     */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /**
     * SHA-256 hex digest of the raw refresh token.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Set when the token is consumed by logout or by rotation.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Hash of the refresh token that replaced this one during
     * rotation. A non-null value means this token was already rotated.
     */
    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

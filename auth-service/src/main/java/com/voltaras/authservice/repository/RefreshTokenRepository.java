package com.voltaras.authservice.repository;

import com.voltaras.authservice.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    /**
     * Token lookup with a pessimistic write lock so concurrent refresh
     * attempts with the same token are serialized: the loser observes
     * the record as already revoked/replaced and is rejected.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from RefreshToken t
            where t.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    List<RefreshToken> findAllByUser_Id(Long userId);

    /**
     * Atomically revokes every active refresh token of one login
     * session, scoped to the authenticated user. Idempotent: a second
     * call simply matches no rows.
     */
    @Modifying
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now
            where t.sessionId = :sessionId
              and t.user.id = :userId
              and t.revokedAt is null
            """)
    int revokeActiveBySessionId(
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("now") LocalDateTime now
    );

    /**
     * Atomically revokes every active refresh session of a user
     * (used after a password reset).
     */
    @Modifying
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now
            where t.user.id = :userId
              and t.revokedAt is null
            """)
    int revokeAllActiveByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * Deletes expired (unused) records and records revoked longer ago
     * than the retention window. Active sessions are never touched.
     */
    @Modifying
    @Query("""
            delete from RefreshToken t
            where (t.expiresAt < :now and t.revokedAt is null)
               or (t.revokedAt is not null
                   and t.revokedAt < :revokedBefore)
            """)
    int deleteExpiredAndOldRevoked(
            @Param("now") LocalDateTime now,
            @Param("revokedBefore") LocalDateTime revokedBefore
    );
}

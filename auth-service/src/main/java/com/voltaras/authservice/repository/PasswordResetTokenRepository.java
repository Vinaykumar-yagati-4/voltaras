package com.voltaras.authservice.repository;

import com.voltaras.authservice.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Token lookup with a pessimistic write lock so concurrent
     * reset attempts with the same token are serialized and the token
     * can only be consumed once.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from PasswordResetToken t
            where t.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    List<PasswordResetToken> findAllByUser_Id(Long userId);

    /**
     * Invalidates every previous unused token of a user before a new
     * one is issued (used tokens are kept as a record).
     */
    void deleteByUser_IdAndUsedAtIsNull(Long userId);

    /**
     * Deletes every other token of the user except the one just
     * consumed during a password reset.
     */
    void deleteByUser_IdAndIdNot(Long userId, Long tokenId);
}

package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data repository for {@link Wallet} entities.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Finds a wallet by owner user ID.
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Finds a wallet by owner user ID, locking the row with a pessimistic
     * write lock so concurrent recharges and bill payments against the same
     * wallet are serialized inside the enclosing transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}

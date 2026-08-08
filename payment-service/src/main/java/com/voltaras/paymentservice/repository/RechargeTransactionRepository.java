package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.RechargeTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link RechargeTransaction} entities
 * (Razorpay recharge orders).
 */
public interface RechargeTransactionRepository
        extends JpaRepository<RechargeTransaction, Long> {

    /**
     * Finds a recharge by its server-generated recharge reference.
     */
    Optional<RechargeTransaction> findByRechargeReference(String rechargeReference);

    /**
     * Finds a recharge by its Razorpay order ID. Uniqueness is also
     * enforced by the {@code uk_recharge_transactions_order_id} constraint.
     */
    Optional<RechargeTransaction> findByOrderId(String orderId);

    /**
     * Finds a recharge by its Razorpay order ID with a pessimistic write
     * lock. Used by the webhook path so concurrent deliveries of the same
     * event are serialized and the wallet can never be credited twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RechargeTransaction r where r.orderId = :orderId")
    Optional<RechargeTransaction> findByOrderIdForUpdate(
            @Param("orderId") String orderId);

    /**
     * Finds a recharge by its client-supplied idempotency key. Uniqueness
     * is also enforced by the {@code uk_recharge_transactions_idempotency_key}
     * constraint.
     */
    Optional<RechargeTransaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * All recharges made by a user, newest first.
     */
    List<RechargeTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}

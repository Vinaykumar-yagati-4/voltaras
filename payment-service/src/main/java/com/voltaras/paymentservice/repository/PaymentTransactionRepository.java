package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link PaymentTransaction} entities
 * (wallet-funded bill payments).
 */
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Finds a bill payment by its server-generated reference.
     */
    Optional<PaymentTransaction> findByPaymentReference(String paymentReference);

    /**
     * Finds a bill payment by its client-supplied idempotency key.
     * Uniqueness is also enforced by the {@code uk_payments_idempotency_key}
     * constraint.
     */
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * All bill payments for a bill, newest first.
     */
    List<PaymentTransaction> findByBillIdOrderByCreatedAtDesc(Long billId);

    /**
     * All bill payments made by a user, newest first (paginated).
     */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(
            Long userId, Pageable pageable);

    /**
     * All bill payments for an organization, newest first (paginated).
     */
    Page<PaymentTransaction> findByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId, Pageable pageable);

    /**
     * All bill payments, newest first (paginated, admin view).
     */
    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

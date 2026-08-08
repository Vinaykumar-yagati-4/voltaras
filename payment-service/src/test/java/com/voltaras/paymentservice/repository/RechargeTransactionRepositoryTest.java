package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.RechargeTransaction;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository tests for {@link RechargeTransactionRepository} using the
 * in-memory H2 database in MySQL mode.
 */
@DataJpaTest
class RechargeTransactionRepositoryTest {

    @Autowired
    private RechargeTransactionRepository rechargeRepository;

    @Test
    @DisplayName("Find by Razorpay order ID")
    void findByOrderId_returnsRecharge() {

        RechargeTransaction saved =
                rechargeRepository.save(buildRecharge("RCH-100", "order_100", "KEY-100"));

        Optional<RechargeTransaction> found =
                rechargeRepository.findByOrderId("order_100");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Find by idempotency key")
    void findByIdempotencyKey_returnsRecharge() {

        rechargeRepository.save(buildRecharge("RCH-200", "order_200", "KEY-200"));

        Optional<RechargeTransaction> found =
                rechargeRepository.findByIdempotencyKey("KEY-200");

        assertThat(found).isPresent();
        assertThat(found.get().getRechargeReference()).isEqualTo("RCH-200");
    }

    @Test
    @DisplayName("Duplicate Razorpay order ID violates the unique constraint")
    void duplicateOrderId_throwsDataIntegrityViolation() {

        rechargeRepository.save(buildRecharge("RCH-300", "order_300", "KEY-300"));

        assertThatThrownBy(() ->
                rechargeRepository.save(buildRecharge("RCH-301", "order_300", "KEY-301")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Duplicate idempotency key violates the unique constraint")
    void duplicateIdempotencyKey_throwsDataIntegrityViolation() {

        rechargeRepository.save(buildRecharge("RCH-400", "order_400", "KEY-400"));

        assertThatThrownBy(() ->
                rechargeRepository.save(buildRecharge("RCH-401", "order_401", "KEY-400")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("List recharges for a user, newest first")
    void findByUserIdOrderByCreatedAtDesc_returnsHistory() {

        rechargeRepository.save(buildRecharge("RCH-500", "order_500", "KEY-500"));
        rechargeRepository.save(buildRecharge("RCH-501", "order_501", "KEY-501"));

        List<RechargeTransaction> recharges =
                rechargeRepository.findByUserIdOrderByCreatedAtDesc(100L);

        assertThat(recharges).hasSize(2);
        assertThat(recharges)
                .extracting(RechargeTransaction::getUserId)
                .containsExactly(100L, 100L);
    }

    private RechargeTransaction buildRecharge(
            String reference, String orderId, String idempotencyKey) {

        return RechargeTransaction.builder()
                .rechargeReference(reference)
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .userId(100L)
                .organizationId(6L)
                .amount(new BigDecimal("500.00"))
                .currency(Currency.INR)
                .paymentMethod(PaymentMethod.UPI)
                .provider(PaymentProvider.RAZORPAY)
                .status(PaymentStatus.CREATED)
                .build();
    }
}

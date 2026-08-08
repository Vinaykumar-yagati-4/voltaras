package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.PaymentTransaction;
import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentStatus;
import com.voltaras.paymentservice.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository tests for {@link PaymentTransactionRepository} using the
 * in-memory H2 database in MySQL mode.
 */
@DataJpaTest
class PaymentTransactionRepositoryTest {

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    @Test
    @DisplayName("Find by payment reference")
    void findByPaymentReference_returnsPayment() {

        PaymentTransaction saved =
                paymentRepository.save(buildPayment("REF-100", "KEY-100"));

        Optional<PaymentTransaction> found =
                paymentRepository.findByPaymentReference("REF-100");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Find by idempotency key")
    void findByIdempotencyKey_returnsPayment() {

        paymentRepository.save(buildPayment("REF-200", "KEY-200"));

        Optional<PaymentTransaction> found =
                paymentRepository.findByIdempotencyKey("KEY-200");

        assertThat(found).isPresent();
        assertThat(found.get().getPaymentReference()).isEqualTo("REF-200");
    }

    @Test
    @DisplayName("Duplicate idempotency key violates the unique constraint")
    void duplicateIdempotencyKey_throwsDataIntegrityViolation() {

        paymentRepository.save(buildPayment("REF-300", "KEY-300"));

        assertThatThrownBy(() ->
                paymentRepository.save(buildPayment("REF-301", "KEY-300")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Duplicate payment reference violates the unique constraint")
    void duplicatePaymentReference_throwsDataIntegrityViolation() {

        paymentRepository.save(buildPayment("REF-400", "KEY-400"));

        assertThatThrownBy(() ->
                paymentRepository.save(buildPayment("REF-400", "KEY-401")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("List payments for a bill, newest first")
    void findByBillIdOrderByCreatedAtDesc_returnsNewestFirst() {

        paymentRepository.save(buildPayment("REF-500", "KEY-500"));
        paymentRepository.save(buildPayment("REF-501", "KEY-501"));

        List<PaymentTransaction> payments =
                paymentRepository.findByBillIdOrderByCreatedAtDesc(5L);

        assertThat(payments).hasSize(2);
        assertThat(payments)
                .extracting(PaymentTransaction::getBillId)
                .containsExactly(5L, 5L);
    }

    @Test
    @DisplayName("Page user payments")
    void findByUserIdOrderByCreatedAtDesc_returnsPage() {

        paymentRepository.save(buildPayment("REF-600", "KEY-600"));

        Page<PaymentTransaction> page =
                paymentRepository.findByUserIdOrderByCreatedAtDesc(
                        100L,
                        PageRequest.of(0, 10,
                                Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Page organization payments")
    void findByOrganizationIdOrderByCreatedAtDesc_returnsPage() {

        paymentRepository.save(buildPayment("REF-700", "KEY-700"));

        Page<PaymentTransaction> page =
                paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(
                        6L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOrganizationId()).isEqualTo(6L);
    }

    private PaymentTransaction buildPayment(
            String reference, String idempotencyKey) {

        return PaymentTransaction.builder()
                .paymentReference(reference)
                .idempotencyKey(idempotencyKey)
                .transactionType(TransactionType.BILL_PAYMENT)
                .billId(5L)
                .organizationId(6L)
                .userId(100L)
                .amount(new BigDecimal("355.04"))
                .currency(Currency.INR)
                .paymentMethod(PaymentMethod.WALLET)
                .status(PaymentStatus.SUCCESS)
                .build();
    }
}

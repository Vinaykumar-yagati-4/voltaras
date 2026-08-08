package com.voltaras.paymentservice.repository;

import com.voltaras.paymentservice.entity.Wallet;
import com.voltaras.paymentservice.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository tests for {@link WalletRepository} using the in-memory H2
 * database in MySQL mode.
 */
@DataJpaTest
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Test
    @DisplayName("Find wallet by user ID")
    void findByUserId_returnsWallet() {

        Wallet saved = walletRepository.save(buildWallet(100L, "100.00"));

        Optional<Wallet> found = walletRepository.findByUserId(100L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Find wallet by user ID with pessimistic lock")
    void findByUserIdForUpdate_returnsWallet() {

        walletRepository.save(buildWallet(101L, "200.00"));

        Optional<Wallet> found = walletRepository.findByUserIdForUpdate(101L);

        assertThat(found).isPresent();
        assertThat(found.get().getBalance())
                .isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("One wallet per user is enforced by the unique constraint")
    void duplicateUserId_throwsDataIntegrityViolation() {

        walletRepository.save(buildWallet(102L, "0.00"));

        assertThatThrownBy(() ->
                walletRepository.save(buildWallet(102L, "50.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Wallet buildWallet(Long userId, String balance) {

        return Wallet.builder()
                .userId(userId)
                .balance(new BigDecimal(balance))
                .currency(Currency.INR)
                .build();
    }
}

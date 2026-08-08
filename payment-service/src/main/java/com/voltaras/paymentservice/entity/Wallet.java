package com.voltaras.paymentservice.entity;

import com.voltaras.paymentservice.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prepaid wallet of a VOLTARAS user.
 *
 * <p>
 * The wallet is created lazily the first time it is accessed (balance 0).
 * Users recharge it through Razorpay and pay bills from it. Reads and
 * updates use a pessimistic write lock so concurrent recharges and bill
 * payments cannot lose money.
 * </p>
 *
 * <p>
 * Monetary fields use {@link BigDecimal} with scale 2. Sensitive payment
 * data is never stored; the wallet only holds a balance.
 * </p>
 */
@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallets_user_id",
                        columnNames = "user_id"
                )
        },
        indexes = {
                @Index(name = "idx_wallets_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auth Service user ID who owns the wallet (unique).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (balance == null) {
            balance = BigDecimal.ZERO.setScale(2);
        }

        if (currency == null) {
            currency = Currency.INR;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

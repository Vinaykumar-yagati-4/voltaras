package com.voltaras.paymentservice.entity;

import com.voltaras.paymentservice.enums.Currency;
import com.voltaras.paymentservice.enums.PaymentMethod;
import com.voltaras.paymentservice.enums.PaymentProvider;
import com.voltaras.paymentservice.enums.PaymentStatus;
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
 * A wallet recharge order created at the Razorpay payment gateway
 * (sandbox/test mode) and confirmed through a signature-protected webhook.
 *
 * <p>
 * Only safe fields are stored. Card numbers, CVV values, UPI PINs and bank
 * credentials are never accepted or persisted. The gateway amount is
 * expressed in paise during the API call and converted back to rupees for
 * storage.
 * </p>
 */
@Entity
@Table(
        name = "recharge_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recharge_transactions_reference",
                        columnNames = "recharge_reference"
                ),
                @UniqueConstraint(
                        name = "uk_recharge_transactions_order_id",
                        columnNames = "order_id"
                ),
                @UniqueConstraint(
                        name = "uk_recharge_transactions_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(name = "idx_recharge_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_recharge_transactions_organization_id", columnList = "organization_id"),
                @Index(name = "idx_recharge_transactions_status", columnList = "payment_status"),
                @Index(name = "idx_recharge_transactions_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Server-generated, immutable recharge reference exposed to clients.
     */
    @Column(name = "recharge_reference", nullable = false, length = 50)
    private String rechargeReference;

    /**
     * Razorpay order ID returned by the gateway (for example
     * {@code order_xxxxxxxxxxxx}). Unique.
     */
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    /**
     * Client-supplied idempotency key; unique across all recharges.
     */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    /**
     * Razorpay payment ID received from the webhook (for example
     * {@code pay_xxxxxxxxxxxx}). Never accepted from the client.
     */
    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = PaymentStatus.CREATED;
        }

        if (provider == null) {
            provider = PaymentProvider.RAZORPAY;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

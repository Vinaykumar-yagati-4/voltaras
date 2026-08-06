package com.voltaras.billservice.entity;

import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.enums.PaymentStatus;
import com.voltaras.billservice.util.BillCalculator;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Electricity bill generated from a verified meter reading.
 *
 * <p>
 * Monetary and meter-reading fields use {@link BigDecimal} with scale 2.
 * All amount arithmetic is performed through {@link BillCalculator} which
 * applies {@link java.math.RoundingMode#HALF_UP}.
 * </p>
 *
 * <p>
 * Duplicate bills for the same user, meter number, billing month and
 * billing year are prevented by a database unique constraint as well as
 * an application-level check in the service layer.
 * </p>
 */
@Entity
@Table(
        name = "bills",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bills_duplicate_period",
                columnNames = {
                        "auth_user_id",
                        "meter_number",
                        "billing_month",
                        "billing_year"
                }
        ),
        indexes = {
                @Index(name = "idx_bills_auth_user_id", columnList = "auth_user_id"),
                @Index(name = "idx_bills_bill_status", columnList = "bill_status"),
                @Index(name = "idx_bills_payment_status", columnList = "payment_status"),
                @Index(name = "idx_bills_billing_period", columnList = "billing_month, billing_year")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External Auth Service user ID received from the API Gateway.
     * No JPA relationship is created to the Auth Service database.
     */
    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    /**
     * ID of the verified meter reading this bill was generated from.
     */
    @Column(name = "meter_reading_id", nullable = false)
    private Long meterReadingId;

    @Column(name = "meter_number", nullable = false, length = 50)
    private String meterNumber;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "previous_reading", nullable = false, precision = 14, scale = 2)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentReading;

    @Column(name = "units_consumed", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitsConsumed;

    @Column(name = "energy_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal energyCharge;

    @Column(name = "fixed_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedCharge;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "late_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "outstanding_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status", nullable = false, length = 20)
    private BillStatus billStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    /**
     * Auth user ID of the administrator who generated this bill.
     */
    @Column(name = "generated_by", nullable = false)
    private Long generatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (generatedDate == null) {
            generatedDate = LocalDate.now();
        }

        if (billStatus == null) {
            billStatus = BillStatus.GENERATED;
        }

        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }

        if (amountPaid == null) {
            amountPaid = BillCalculator.ZERO;
        }

        if (lateFee == null) {
            lateFee = BillCalculator.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BillCalculator.ZERO;
        }

        if (outstandingAmount == null) {
            outstandingAmount = totalAmount;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

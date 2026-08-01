package com.voltaras.meterreadingservice.entity;

import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents one daily electricity meter reading submitted by a consumer.
 *
 * Monthly reporting is supported through billingMonth and billingYear,
 * which are derived from readingDate by the service layer.
 *
 * A consumer can submit only one reading per meter for a particular date.
 */
@Entity
@Table(
        name = "meter_readings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meter_reading_daily",
                columnNames = {
                        "auth_user_id",
                        "meter_number",
                        "reading_date"
                }
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Authenticated consumer ID received from X-User-Id.
     */
    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Column(name = "meter_number", nullable = false, length = 50)
    private String meterNumber;

    /**
     * Actual date on which the reading was recorded.
     */
    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    /**
     * Derived automatically from readingDate.
     */
    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    /**
     * Derived automatically from readingDate.
     */
    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(
            name = "previous_reading",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal previousReading;

    @Column(
            name = "current_reading",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal currentReading;

    /**
     * Calculated automatically:
     * currentReading - previousReading.
     */
    @Column(
            name = "units_consumed",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal unitsConsumed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeterReadingStatus status;

    @Column(length = 500)
    private String remarks;

    /**
     * Admin user ID who verified or rejected the reading.
     */
    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
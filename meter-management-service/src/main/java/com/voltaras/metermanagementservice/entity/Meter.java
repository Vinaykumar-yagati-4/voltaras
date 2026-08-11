package com.voltaras.metermanagementservice.entity;

import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Master record of a physical electricity meter.
 *
 * <p>
 * This entity is the source of truth for meter details, ownership,
 * assignment and status. Monthly consumption values are NOT stored here;
 * they belong to the Meter Reading Service.
 * </p>
 */
@Entity
@Table(
        name = "meters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meter_number",
                columnNames = "meter_number"
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique physical meter number printed on the meter body.
     */
    @Column(name = "meter_number", nullable = false, unique = true, length = 50)
    private String meterNumber;

    /**
     * Consumer the meter is currently assigned to, or null when unassigned.
     */
    @Column(name = "auth_user_id")
    private Long authUserId;

    /**
     * Organization the meter belongs to, or null when not organization-owned.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 20)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 20)
    private ConnectionType connectionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false, length = 20)
    private PhaseType phaseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeterStatus status;

    @Column(name = "sanctioned_load_kw", precision = 8, scale = 3)
    private BigDecimal sanctionedLoadKw;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
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

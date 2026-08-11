package com.voltaras.metermanagementservice.repository;

import com.voltaras.metermanagementservice.entity.Meter;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    Optional<Meter> findByMeterNumber(String meterNumber);

    /**
     * Ownership-scoped lookup: only returns the meter when it is assigned
     * to the given consumer.
     */
    Optional<Meter> findByIdAndAuthUserId(Long id, Long authUserId);

    List<Meter> findAllByAuthUserIdOrderByCreatedAtDesc(Long authUserId);

    List<Meter> findAllByStatusOrderByCreatedAtDesc(MeterStatus status);

    List<Meter> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    /**
     * Admin listing with optional filters. Every parameter is optional;
     * null values are ignored so only the provided filters are applied.
     */
    @Query("""
            SELECT m FROM Meter m
            WHERE (:status IS NULL OR m.status = :status)
              AND (:authUserId IS NULL OR m.authUserId = :authUserId)
              AND (:organizationId IS NULL OR m.organizationId = :organizationId)
              AND (:meterNumber IS NULL OR m.meterNumber = :meterNumber)
            ORDER BY m.createdAt DESC
            """)
    List<Meter> findAllByFilters(
            @Param("status") MeterStatus status,
            @Param("authUserId") Long authUserId,
            @Param("organizationId") Long organizationId,
            @Param("meterNumber") String meterNumber
    );
}

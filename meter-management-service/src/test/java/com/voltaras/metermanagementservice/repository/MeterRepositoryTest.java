package com.voltaras.metermanagementservice.repository;

import com.voltaras.metermanagementservice.entity.Meter;
import com.voltaras.metermanagementservice.enums.ConnectionType;
import com.voltaras.metermanagementservice.enums.MeterStatus;
import com.voltaras.metermanagementservice.enums.MeterType;
import com.voltaras.metermanagementservice.enums.PhaseType;
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
 * Repository-layer tests for {@link MeterRepository} running against the
 * in-memory H2 database.
 */
@DataJpaTest
class MeterRepositoryTest {

    @Autowired
    private MeterRepository meterRepository;

    @Test
    @DisplayName("Save and find meter by meter number")
    void saveAndFindByMeterNumber() {

        Meter saved = meterRepository.save(buildMeter("MTR-001", null, MeterStatus.ACTIVE));

        Optional<Meter> found = meterRepository.findByMeterNumber("MTR-001");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getMeterType()).isEqualTo(MeterType.SMART);
    }

    @Test
    @DisplayName("existsByMeterNumber returns true for an existing number")
    void existsByMeterNumber_trueWhenPresent() {

        meterRepository.save(buildMeter("MTR-002", null, MeterStatus.ACTIVE));

        assertThat(meterRepository.existsByMeterNumber("MTR-002")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-999")).isFalse();
    }

    @Test
    @DisplayName("findByIdAndAuthUserId returns the meter only when owned by the user")
    void findByIdAndAuthUserId_scopesByOwner() {

        Meter owned = meterRepository.save(buildMeter("MTR-003", 100L, MeterStatus.ACTIVE));
        meterRepository.save(buildMeter("MTR-004", 200L, MeterStatus.ACTIVE));

        Optional<Meter> found = meterRepository.findByIdAndAuthUserId(owned.getId(), 100L);

        assertThat(found).isPresent();
        assertThat(found.get().getAuthUserId()).isEqualTo(100L);

        assertThat(meterRepository.findByIdAndAuthUserId(owned.getId(), 999L)).isEmpty();
    }

    @Test
    @DisplayName("findAllByAuthUserId returns only the user's meters")
    void findAllByAuthUserId_filtersByOwner() {

        meterRepository.save(buildMeter("MTR-005", 100L, MeterStatus.ACTIVE));
        meterRepository.save(buildMeter("MTR-006", 100L, MeterStatus.INACTIVE));
        meterRepository.save(buildMeter("MTR-007", 200L, MeterStatus.ACTIVE));

        List<Meter> meters = meterRepository.findAllByAuthUserIdOrderByCreatedAtDesc(100L);

        assertThat(meters).hasSize(2);
        assertThat(meters).extracting(Meter::getMeterNumber)
                .containsExactlyInAnyOrder("MTR-005", "MTR-006");
    }

    @Test
    @DisplayName("findAllByStatus and findAllByOrganizationId filter correctly")
    void singleFilterFinders() {

        meterRepository.save(buildMeter("MTR-008", null, MeterStatus.FAULTY, 7L));
        meterRepository.save(buildMeter("MTR-009", null, MeterStatus.ACTIVE));
        meterRepository.save(buildMeter("MTR-010", null, MeterStatus.FAULTY));

        assertThat(meterRepository.findAllByStatusOrderByCreatedAtDesc(MeterStatus.FAULTY))
                .hasSize(2);

        List<Meter> byOrg = meterRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(7L);

        assertThat(byOrg).hasSize(1);
        assertThat(byOrg.getFirst().getMeterNumber()).isEqualTo("MTR-008");
    }

    @Test
    @DisplayName("findAllByFilters combines optional filters (nulls ignored)")
    void findAllByFilters_combinesOptionalFilters() {

        meterRepository.save(buildMeter("MTR-011", 100L, MeterStatus.ACTIVE, 7L));
        meterRepository.save(buildMeter("MTR-012", 100L, MeterStatus.INACTIVE, 7L));
        meterRepository.save(buildMeter("MTR-013", 200L, MeterStatus.ACTIVE, 9L));

        // No filters -> all meters
        assertThat(meterRepository.findAllByFilters(null, null, null, null)).hasSize(3);

        // Single filter
        assertThat(meterRepository.findAllByFilters(MeterStatus.ACTIVE, null, null, null))
                .hasSize(2);

        // Combined filters
        List<Meter> combined = meterRepository.findAllByFilters(
                MeterStatus.ACTIVE, 100L, 7L, "MTR-011");

        assertThat(combined).hasSize(1);
        assertThat(combined.getFirst().getMeterNumber()).isEqualTo("MTR-011");
    }

    @Test
    @DisplayName("Duplicate meter number violates the unique constraint")
    void duplicateMeterNumber_violatesUniqueConstraint() {

        meterRepository.save(buildMeter("MTR-014", null, MeterStatus.ACTIVE));

        assertThatThrownBy(() ->
                meterRepository.saveAndFlush(
                        buildMeter("MTR-014", null, MeterStatus.ACTIVE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Meter buildMeter(String meterNumber, Long authUserId, MeterStatus status) {
        return buildMeter(meterNumber, authUserId, status, null);
    }

    private Meter buildMeter(
            String meterNumber, Long authUserId, MeterStatus status, Long organizationId) {

        return Meter.builder()
                .meterNumber(meterNumber)
                .authUserId(authUserId)
                .organizationId(organizationId)
                .meterType(MeterType.SMART)
                .connectionType(ConnectionType.RESIDENTIAL)
                .phaseType(PhaseType.SINGLE_PHASE)
                .status(status)
                .sanctionedLoadKw(new BigDecimal("5.000"))
                .city("Bengaluru")
                .pincode("560001")
                .build();
    }
}

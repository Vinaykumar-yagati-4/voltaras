package com.voltaras.meterreadingservice.repository;

import com.voltaras.meterreadingservice.entity.MeterReading;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository
        extends JpaRepository<MeterReading, Long> {

    boolean existsByAuthUserIdAndMeterNumberAndReadingDate(
            Long authUserId,
            String meterNumber,
            LocalDate readingDate
    );

    boolean existsByAuthUserIdAndMeterNumberAndReadingDateAndIdNot(
            Long authUserId,
            String meterNumber,
            LocalDate readingDate,
            Long id
    );


    Optional<MeterReading> findByIdAndAuthUserId(
            Long id,
            Long authUserId
    );


    List<MeterReading> findAllByAuthUserIdOrderByReadingDateDesc(
            Long authUserId
    );


    List<MeterReading> findAllByStatusOrderByCreatedAtDesc(
            MeterReadingStatus status
    );

    List<MeterReading> findAllByOrderByCreatedAtDesc();
}
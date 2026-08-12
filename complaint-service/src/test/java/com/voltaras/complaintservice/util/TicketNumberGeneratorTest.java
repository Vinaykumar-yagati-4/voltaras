package com.voltaras.complaintservice.util;

import com.voltaras.complaintservice.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for the per-day ticket number generator.
 */
@ExtendWith(MockitoExtension.class)
class TicketNumberGeneratorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    @Mock
    private ComplaintRepository complaintRepository;

    private TicketNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TicketNumberGenerator(complaintRepository);
    }

    @Test
    @DisplayName("First complaint of the day gets sequence 0001")
    void firstTicketOfDay() {
        when(complaintRepository.countByTicketNumberStartingWith("CMP-20260812-"))
                .thenReturn(0L);

        assertThat(generator.generateNextTicketNumber(DATE))
                .isEqualTo("CMP-20260812-0001");
    }

    @Test
    @DisplayName("Sequence continues from the existing per-day count")
    void sequenceContinues() {
        when(complaintRepository.countByTicketNumberStartingWith("CMP-20260812-"))
                .thenReturn(12L);

        assertThat(generator.generateNextTicketNumber(DATE))
                .isEqualTo("CMP-20260812-0013");
    }
}

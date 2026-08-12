package com.voltaras.complaintservice.util;

import com.voltaras.complaintservice.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates stable, unique complaint ticket numbers in the documented
 * format {@code CMP-YYYYMMDD-NNNN} (per-day sequence). The unique
 * constraint on {@code complaints.ticket_number} guards against concurrent
 * duplicates; a violation surfaces as {@code 409 DATA_CONSTRAINT_VIOLATION}.
 */
@Component
@RequiredArgsConstructor
public class TicketNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    private final ComplaintRepository complaintRepository;

    /**
     * @return the next ticket number for today, e.g. {@code CMP-20260812-0001}
     */
    public String generateNextTicketNumber() {

        return generateNextTicketNumber(LocalDate.now());
    }

    /**
     * Testable variant with an explicit date.
     */
    String generateNextTicketNumber(LocalDate date) {

        String prefix = "CMP-" + DATE_FORMAT.format(date) + "-";

        long sequence = complaintRepository
                .countByTicketNumberStartingWith(prefix) + 1;

        return prefix + String.format("%04d", sequence);
    }
}

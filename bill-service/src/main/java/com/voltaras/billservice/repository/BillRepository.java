package com.voltaras.billservice.repository;

import com.voltaras.billservice.entity.Bill;
import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Bill} entities.
 */
public interface BillRepository extends JpaRepository<Bill, Long> {

    // ------------------------------------------------------------------
    // Consumer queries
    // ------------------------------------------------------------------

    /**
     * Full bill history of a consumer, newest first.
     */
    List<Bill> findByAuthUserIdOrderByCreatedAtDesc(Long authUserId);

    /**
     * Finds a bill only when it belongs to the given user. Used to
     * enforce consumer ownership.
     */
    Optional<Bill> findByIdAndAuthUserId(Long id, Long authUserId);

    /**
     * Consumer bill history filtered by billing month and year.
     */
    List<Bill> findByAuthUserIdAndBillingMonthAndBillingYearOrderByCreatedAtDesc(
            Long authUserId,
            Integer billingMonth,
            Integer billingYear
    );

    /**
     * Consumer bills that are still payable (not PAID, not REFUNDED and
     * not CANCELLED), newest first.
     */
    @Query("""
            SELECT b FROM Bill b
            WHERE b.authUserId = :authUserId
              AND b.billStatus <> :cancelledStatus
              AND b.paymentStatus NOT IN :settledStatuses
            ORDER BY b.createdAt DESC
            """)
    List<Bill> findOutstandingBillsByAuthUserId(
            @Param("authUserId") Long authUserId,
            @Param("cancelledStatus") BillStatus cancelledStatus,
            @Param("settledStatuses") List<PaymentStatus> settledStatuses
    );

    // ------------------------------------------------------------------
    // Duplicate prevention
    // ------------------------------------------------------------------

    /**
     * True when a bill already exists for the same user, meter number,
     * billing month and billing year.
     */
    boolean existsByAuthUserIdAndMeterNumberAndBillingMonthAndBillingYear(
            Long authUserId,
            String meterNumber,
            Integer billingMonth,
            Integer billingYear
    );

    // ------------------------------------------------------------------
    // Admin queries
    // ------------------------------------------------------------------

    /**
     * Admin dashboard query with all filters optional.
     */
    @Query("""
            SELECT b FROM Bill b
            WHERE (:status IS NULL OR b.billStatus = :status)
              AND (:month IS NULL OR b.billingMonth = :month)
              AND (:year IS NULL OR b.billingYear = :year)
            ORDER BY b.createdAt DESC
            """)
    List<Bill> findAdminFiltered(
            @Param("status") BillStatus status,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    /**
     * Bills in the given status.
     */
    List<Bill> findByBillStatus(BillStatus billStatus);

    /**
     * Bills in the given payment status.
     */
    List<Bill> findByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Bills generated for a specific billing period.
     */
    List<Bill> findByBillingMonthAndBillingYear(
            Integer billingMonth,
            Integer billingYear
    );

    /**
     * Candidates for the mark-overdue job: still payable bills whose due
     * date is before the given date.
     */
    @Query("""
            SELECT b FROM Bill b
            WHERE b.billStatus IN :activeStatuses
              AND b.paymentStatus NOT IN :settledStatuses
              AND b.dueDate < :today
            """)
    List<Bill> findOverdueCandidates(
            @Param("activeStatuses") List<BillStatus> activeStatuses,
            @Param("settledStatuses") List<PaymentStatus> settledStatuses,
            @Param("today") LocalDate today
    );
}

package com.voltaras.billservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * Unit tests for {@link BillCalculator}.
 */
class BillCalculatorTest {

    private static final BigDecimal DELTA = new BigDecimal("0.001");

    // ------------------------------------------------------------------
    // Units consumed
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Units consumed = current reading - previous reading")
    void calculateUnitsConsumed_difference() {

        BigDecimal units = BillCalculator.calculateUnitsConsumed(
                new BigDecimal("1250.50"),
                new BigDecimal("1385.75")
        );

        assertThat(units).isEqualByComparingTo(new BigDecimal("135.25"));
    }

    @Test
    @DisplayName("Zero consumption when readings are equal")
    void calculateUnitsConsumed_equalReadings_zero() {

        BigDecimal units = BillCalculator.calculateUnitsConsumed(
                new BigDecimal("1000"),
                new BigDecimal("1000")
        );

        assertThat(units).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------
    // Tariff slabs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Tariff: 0-100 units billed at 1.50 per unit")
    void calculateEnergyCharge_firstSlab() {

        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("100")))
                .isEqualByComparingTo(new BigDecimal("150.00"));

        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("50")))
                .isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("Tariff: 101-200 units adds 2.50 per unit")
    void calculateEnergyCharge_secondSlab() {

        // 100 * 1.50 + 100 * 2.50 = 400.00
        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("200")))
                .isEqualByComparingTo(new BigDecimal("400.00"));

        // 100 * 1.50 + 50 * 2.50 = 275.00
        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("150")))
                .isEqualByComparingTo(new BigDecimal("275.00"));
    }

    @Test
    @DisplayName("Tariff: 201-300 units adds 4.00 per unit")
    void calculateEnergyCharge_thirdSlab() {

        // 150 + 250 + 100 * 4.00 = 800.00
        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("300")))
                .isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("Tariff: above 300 units billed at 6.00 per unit")
    void calculateEnergyCharge_fourthSlab() {

        // 150 + 250 + 400 + 50 * 6.00 = 1100.00
        assertThat(BillCalculator.calculateEnergyCharge(new BigDecimal("350")))
                .isEqualByComparingTo(new BigDecimal("1100.00"));
    }

    @Test
    @DisplayName("Tariff: negative units rejected")
    void calculateEnergyCharge_negativeUnits_throws() {

        assertThatThrownBy(() ->
                BillCalculator.calculateEnergyCharge(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitsConsumed");
    }

    // ------------------------------------------------------------------
    // Tax
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Tax = 5% of (energy charge + fixed charge)")
    void calculateTaxAmount_fivePercent() {

        BigDecimal tax = BillCalculator.calculateTaxAmount(
                new BigDecimal("150.00"),
                new BigDecimal("100.00")
        );

        // 250.00 * 0.05 = 12.50
        assertThat(tax).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    // ------------------------------------------------------------------
    // Total amount
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Total = energy + fixed + tax + lateFee - discount")
    void calculateTotalAmount_combinesAllCharges() {

        BigDecimal total = BillCalculator.calculateTotalAmount(
                new BigDecimal("150.00"), // energy
                new BigDecimal("100.00"), // fixed
                new BigDecimal("12.50"),  // tax
                new BigDecimal("0.00"),   // late fee
                new BigDecimal("0.00")    // discount
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("262.50"));
    }

    @Test
    @DisplayName("Total includes late fee and applies discount")
    void calculateTotalAmount_withLateFeeAndDiscount() {

        BigDecimal total = BillCalculator.calculateTotalAmount(
                new BigDecimal("150.00"),
                new BigDecimal("100.00"),
                new BigDecimal("12.50"),
                new BigDecimal("50.00"),
                new BigDecimal("25.00")
        );

        // 150 + 100 + 12.50 + 50 - 25 = 287.50
        assertThat(total).isEqualByComparingTo(new BigDecimal("287.50"));
    }

    @Test
    @DisplayName("Total is rounded half-up to scale 2")
    void calculateTotalAmount_roundsHalfUp() {

        BigDecimal total = BillCalculator.calculateTotalAmount(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("10.005"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00")
        );

        assertThat(total).isCloseTo(new BigDecimal("210.01"), offset(DELTA));
    }

    // ------------------------------------------------------------------
    // Outstanding amount
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Outstanding = total - amount paid")
    void calculateOutstandingAmount_difference() {

        BigDecimal outstanding = BillCalculator.calculateOutstandingAmount(
                new BigDecimal("262.50"),
                new BigDecimal("100.00")
        );

        assertThat(outstanding).isEqualByComparingTo(new BigDecimal("162.50"));
    }

    @Test
    @DisplayName("Outstanding never negative when overpaid")
    void calculateOutstandingAmount_overpaid_returnsZero() {

        BigDecimal outstanding = BillCalculator.calculateOutstandingAmount(
                new BigDecimal("262.50"),
                new BigDecimal("300.00")
        );

        assertThat(outstanding).isEqualByComparingTo(new BigDecimal("0.00"));
    }
}

package com.voltaras.billservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized tariff and monetary calculation helper for VOLTARAS bills.
 *
 * <p>
 * Tariff slabs (energy charge per unit consumed):
 * </p>
 *
 * <ul>
 *     <li>0 – 100 units: ₹1.50 per unit</li>
 *     <li>101 – 200 units: ₹2.50 per unit</li>
 *     <li>201 – 300 units: ₹4.00 per unit</li>
 *     <li>Above 300 units: ₹6.00 per unit</li>
 * </ul>
 *
 * <p>
 * Additional charges: fixed charge ₹100.00, tax 5% of (energy charge +
 * fixed charge), late fee ₹50.00 when applied, discount defaults to
 * ₹0.00. All monetary results use scale 2 and
 * {@link RoundingMode#HALF_UP}.
 * </p>
 */
public final class BillCalculator {

    /** Uniform monetary scale used by every amount in the system. */
    public static final int MONEY_SCALE = 2;

    /** Fixed charge applied to every bill. */
    public static final BigDecimal FIXED_CHARGE = new BigDecimal("100.00");

    /** Tax percentage applied on energy charge + fixed charge. */
    public static final BigDecimal TAX_RATE = new BigDecimal("0.05");

    /** Late fee applied when a bill becomes overdue. */
    public static final BigDecimal LATE_FEE = new BigDecimal("50.00");

    /** Zero money value used as default for late fee and discount. */
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    /** Upper boundary of the first tariff slab. */
    private static final BigDecimal SLAB_ONE_UPPER = new BigDecimal("100");

    /** Upper boundary of the second tariff slab. */
    private static final BigDecimal SLAB_TWO_UPPER = new BigDecimal("200");

    /** Upper boundary of the third tariff slab. */
    private static final BigDecimal SLAB_THREE_UPPER = new BigDecimal("300");

    private static final BigDecimal RATE_SLAB_ONE = new BigDecimal("1.50");
    private static final BigDecimal RATE_SLAB_TWO = new BigDecimal("2.50");
    private static final BigDecimal RATE_SLAB_THREE = new BigDecimal("4.00");
    private static final BigDecimal RATE_SLAB_FOUR = new BigDecimal("6.00");

    private BillCalculator() {
        // Utility class - no instances.
    }

    /**
     * Calculates units consumed between two meter readings.
     *
     * <p>
     * Callers must validate that {@code currentReading >= previousReading}
     * before invoking this method; a negative result indicates invalid
     * input and is rejected.
     * </p>
     *
     * @param previousReading previous meter reading
     * @param currentReading current meter reading
     * @return consumed units (current - previous), scale 2
     */
    public static BigDecimal calculateUnitsConsumed(
            BigDecimal previousReading,
            BigDecimal currentReading
    ) {

        requireNonNull(previousReading, "previousReading");
        requireNonNull(currentReading, "currentReading");

        return scaleMoney(
                currentReading.subtract(previousReading)
        );
    }

    /**
     * Calculates the energy charge for consumed units using progressive
     * tariff slabs. Only the portion of units inside each slab is billed
     * at that slab's rate.
     *
     * @param unitsConsumed units consumed in the billing period
     * @return energy charge, scale 2, HALF_UP
     */
    public static BigDecimal calculateEnergyCharge(BigDecimal unitsConsumed) {

        requireNonNegative(unitsConsumed, "unitsConsumed");

        BigDecimal charge;

        if (unitsConsumed.compareTo(SLAB_ONE_UPPER) <= 0) {

            charge = unitsConsumed.multiply(RATE_SLAB_ONE);

        } else if (unitsConsumed.compareTo(SLAB_TWO_UPPER) <= 0) {

            charge = SLAB_ONE_UPPER.multiply(RATE_SLAB_ONE)
                    .add(unitsConsumed.subtract(SLAB_ONE_UPPER).multiply(RATE_SLAB_TWO));

        } else if (unitsConsumed.compareTo(SLAB_THREE_UPPER) <= 0) {

            charge = SLAB_ONE_UPPER.multiply(RATE_SLAB_ONE)
                    .add(SLAB_ONE_UPPER.multiply(RATE_SLAB_TWO))
                    .add(unitsConsumed.subtract(SLAB_TWO_UPPER).multiply(RATE_SLAB_THREE));

        } else {

            charge = SLAB_ONE_UPPER.multiply(RATE_SLAB_ONE)
                    .add(SLAB_ONE_UPPER.multiply(RATE_SLAB_TWO))
                    .add(SLAB_ONE_UPPER.multiply(RATE_SLAB_THREE))
                    .add(unitsConsumed.subtract(SLAB_THREE_UPPER).multiply(RATE_SLAB_FOUR));
        }

        return scaleMoney(charge);
    }

    /**
     * Calculates tax as a percentage of (energy charge + fixed charge).
     *
     * @param energyCharge energy charge
     * @param fixedCharge fixed charge
     * @return tax amount, scale 2, HALF_UP
     */
    public static BigDecimal calculateTaxAmount(
            BigDecimal energyCharge,
            BigDecimal fixedCharge
    ) {

        requireNonNegative(energyCharge, "energyCharge");
        requireNonNegative(fixedCharge, "fixedCharge");

        return scaleMoney(
                energyCharge
                        .add(fixedCharge)
                        .multiply(TAX_RATE)
        );
    }

    /**
     * Calculates the final payable amount:
     * energy charge + fixed charge + tax + late fee - discount.
     *
     * @param energyCharge energy charge
     * @param fixedCharge fixed charge
     * @param taxAmount tax amount
     * @param lateFee late fee (0.00 when not applied)
     * @param discountAmount discount (0.00 by default)
     * @return total amount, scale 2, HALF_UP
     */
    public static BigDecimal calculateTotalAmount(
            BigDecimal energyCharge,
            BigDecimal fixedCharge,
            BigDecimal taxAmount,
            BigDecimal lateFee,
            BigDecimal discountAmount
    ) {

        requireNonNegative(energyCharge, "energyCharge");
        requireNonNegative(fixedCharge, "fixedCharge");
        requireNonNegative(taxAmount, "taxAmount");
        requireNonNegative(lateFee, "lateFee");
        requireNonNegative(discountAmount, "discountAmount");

        return scaleMoney(
                energyCharge
                        .add(fixedCharge)
                        .add(taxAmount)
                        .add(lateFee)
                        .subtract(discountAmount)
        );
    }

    /**
     * Calculates the remaining outstanding amount and never returns a
     * negative value (over-payment results in zero outstanding).
     *
     * @param totalAmount total bill amount
     * @param amountPaid amount already paid
     * @return outstanding amount, scale 2, HALF_UP
     */
    public static BigDecimal calculateOutstandingAmount(
            BigDecimal totalAmount,
            BigDecimal amountPaid
    ) {

        requireNonNegative(totalAmount, "totalAmount");
        requireNonNegative(amountPaid, "amountPaid");

        BigDecimal outstanding =
                totalAmount.subtract(amountPaid);

        if (outstanding.signum() < 0) {
            outstanding = ZERO;
        }

        return scaleMoney(outstanding);
    }

    /**
     * Normalizes a monetary value to {@link #MONEY_SCALE} using
     * {@link RoundingMode#HALF_UP}.
     *
     * @param value raw monetary value
     * @return value with scale 2
     */
    public static BigDecimal scaleMoney(BigDecimal value) {

        requireNonNull(value, "value");

        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void requireNonNegative(BigDecimal value, String name) {

        requireNonNull(value, name);

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }
    }

    private static void requireNonNull(BigDecimal value, String name) {

        if (value == null) {
            throw new IllegalArgumentException(
                    name + " must not be null"
            );
        }
    }
}

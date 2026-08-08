package com.voltaras.paymentservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared money helpers. All monetary values use scale 2 with
 * {@link RoundingMode#HALF_UP}, matching the other VOLTARAS services.
 *
 * <p>
 * The Razorpay gateway expresses amounts in paise (1 INR = 100 paise);
 * {@link #toPaise} and {@link #fromPaise} convert at the boundary.
 * </p>
 */
public final class MoneyUtils {

    public static final int SCALE = 2;

    private MoneyUtils() {
    }

    /**
     * Scales a money value to 2 decimal places using HALF_UP.
     */
    public static BigDecimal scale(BigDecimal value) {

        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Converts a rupee amount to paise for the gateway.
     *
     * @param rupees amount in rupees
     * @return amount in paise
     */
    public static long toPaise(BigDecimal rupees) {

        return scale(rupees)
                .movePointRight(2)
                .longValueExact();
    }

    /**
     * Converts a gateway amount in paise back to rupees.
     *
     * @param paise amount in paise
     * @return amount in rupees
     */
    public static BigDecimal fromPaise(long paise) {

        return BigDecimal.valueOf(paise)
                .movePointLeft(2)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }
}

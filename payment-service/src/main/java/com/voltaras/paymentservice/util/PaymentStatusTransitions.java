package com.voltaras.paymentservice.util;

import com.voltaras.paymentservice.enums.PaymentStatus;

import java.util.Map;
import java.util.Set;

/**
 * Defines the only allowed {@link PaymentStatus} transitions.
 *
 * <ul>
 *     <li>CREATED &rarr; PENDING, SUCCESS, FAILED, CANCELLED</li>
 *     <li>PENDING &rarr; SUCCESS, FAILED, CANCELLED</li>
 *     <li>SUCCESS &rarr; REFUNDED</li>
 *     <li>FAILED, CANCELLED and REFUNDED are terminal</li>
 * </ul>
 */
public final class PaymentStatusTransitions {

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS =
            Map.of(
                    PaymentStatus.CREATED,
                    Set.of(
                            PaymentStatus.PENDING,
                            PaymentStatus.SUCCESS,
                            PaymentStatus.FAILED,
                            PaymentStatus.CANCELLED
                    ),
                    PaymentStatus.PENDING,
                    Set.of(
                            PaymentStatus.SUCCESS,
                            PaymentStatus.FAILED,
                            PaymentStatus.CANCELLED
                    ),
                    PaymentStatus.SUCCESS,
                    Set.of(PaymentStatus.REFUNDED),
                    PaymentStatus.FAILED,
                    Set.of(),
                    PaymentStatus.CANCELLED,
                    Set.of(),
                    PaymentStatus.REFUNDED,
                    Set.of()
            );

    private PaymentStatusTransitions() {
    }

    /**
     * @param from current status
     * @param to desired status
     * @return true when the transition is allowed
     */
    public static boolean canTransition(PaymentStatus from, PaymentStatus to) {

        if (from == null || to == null) {
            return false;
        }

        return TRANSITIONS
                .getOrDefault(from, Set.of())
                .contains(to);
    }
}

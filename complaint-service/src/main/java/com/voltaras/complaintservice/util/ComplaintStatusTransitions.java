package com.voltaras.complaintservice.util;

import com.voltaras.complaintservice.enums.ComplaintStatus;

import java.util.Map;
import java.util.Set;

/**
 * Defines the only allowed {@link ComplaintStatus} transitions.
 *
 * <ul>
 *     <li>OPEN &rarr; IN_PROGRESS, RESOLVED</li>
 *     <li>IN_PROGRESS &rarr; RESOLVED</li>
 *     <li>RESOLVED &rarr; CLOSED</li>
 *     <li>CLOSED is terminal; complaints are never cancelled or deleted</li>
 * </ul>
 */
public final class ComplaintStatusTransitions {

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> TRANSITIONS =
            Map.of(
                    ComplaintStatus.OPEN,
                    Set.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED),
                    ComplaintStatus.IN_PROGRESS,
                    Set.of(ComplaintStatus.RESOLVED),
                    ComplaintStatus.RESOLVED,
                    Set.of(ComplaintStatus.CLOSED),
                    ComplaintStatus.CLOSED,
                    Set.of()
            );

    private ComplaintStatusTransitions() {
        // Static utility class; no instances.
    }

    /**
     * @param from current status
     * @param to desired status
     * @return true when the transition is allowed
     */
    public static boolean canTransition(ComplaintStatus from, ComplaintStatus to) {

        if (from == null || to == null) {
            return false;
        }

        if (from == to) {
            // Same-status transitions are rejected explicitly by the service.
            return false;
        }

        return TRANSITIONS
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    /**
     * Human-readable list of allowed targets for error messages.
     */
    public static String allowedTargets(ComplaintStatus from) {

        if (from == null) {
            return "none";
        }

        Set<ComplaintStatus> targets = TRANSITIONS.getOrDefault(from, Set.of());

        if (targets.isEmpty()) {
            return "none (" + from + " is terminal)";
        }

        return String.join(", ",
                targets.stream().map(Enum::name).toList());
    }
}

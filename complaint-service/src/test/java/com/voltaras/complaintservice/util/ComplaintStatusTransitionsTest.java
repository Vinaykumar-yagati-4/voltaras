package com.voltaras.complaintservice.util;

import com.voltaras.complaintservice.enums.ComplaintStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the deterministic complaint status transition table.
 */
class ComplaintStatusTransitionsTest {

    @Test
    @DisplayName("OPEN -> IN_PROGRESS is allowed")
    void openToInProgress() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    @DisplayName("OPEN -> RESOLVED is allowed")
    void openToResolved() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.OPEN, ComplaintStatus.RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("IN_PROGRESS -> RESOLVED is allowed")
    void inProgressToResolved() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("RESOLVED -> CLOSED is allowed")
    void resolvedToClosed() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("CLOSED is terminal")
    void closedIsTerminal() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.CLOSED, ComplaintStatus.OPEN)).isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS)).isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.CLOSED, ComplaintStatus.RESOLVED)).isFalse();
    }

    @Test
    @DisplayName("Invalid forward jumps are rejected (OPEN -> CLOSED, IN_PROGRESS -> CLOSED)")
    void invalidJumps() {
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.OPEN, ComplaintStatus.CLOSED)).isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.IN_PROGRESS, ComplaintStatus.CLOSED)).isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.IN_PROGRESS, ComplaintStatus.OPEN)).isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.RESOLVED, ComplaintStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    @DisplayName("Same-status transitions are rejected")
    void sameStatusRejected() {
        for (ComplaintStatus status : ComplaintStatus.values()) {
            assertThat(ComplaintStatusTransitions.canTransition(status, status))
                    .as("same-status %s", status)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Null inputs are rejected")
    void nullInputs() {
        assertThat(ComplaintStatusTransitions.canTransition(null, ComplaintStatus.OPEN))
                .isFalse();
        assertThat(ComplaintStatusTransitions.canTransition(
                ComplaintStatus.OPEN, null)).isFalse();
    }

    @Test
    @DisplayName("allowedTargets lists the possible next states")
    void allowedTargetsMessage() {
        assertThat(ComplaintStatusTransitions.allowedTargets(ComplaintStatus.OPEN))
                .contains("IN_PROGRESS", "RESOLVED");
        assertThat(ComplaintStatusTransitions.allowedTargets(ComplaintStatus.CLOSED))
                .contains("terminal");
    }
}

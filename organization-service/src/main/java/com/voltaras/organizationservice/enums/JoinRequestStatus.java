package com.voltaras.organizationservice.enums;

/**
 * Status of a request to join an organization.
 * <ul>
 *   <li>{@link #PENDING} — awaiting review by an OWNER or ORGANIZATION_ADMIN.</li>
 *   <li>{@link #APPROVED} — approved; a membership is created for the requesting user.</li>
 *   <li>{@link #REJECTED} — rejected by an OWNER or ORGANIZATION_ADMIN (with remarks).</li>
 *   <li>{@link #CANCELLED} — withdrawn by the requesting user.</li>
 * </ul>
 */
public enum JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

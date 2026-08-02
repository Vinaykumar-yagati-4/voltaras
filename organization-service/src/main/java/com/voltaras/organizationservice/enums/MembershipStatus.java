package com.voltaras.organizationservice.enums;

/**
 * Status of a user's membership in an organization.
 * <ul>
 *   <li>{@link #ACTIVE} — membership is active; role-based access applies.</li>
 *   <li>{@link #SUSPENDED} — membership is temporarily suspended by an administrator.</li>
 *   <li>{@link #REMOVED} — membership has been removed; the user is no longer a member.</li>
 * </ul>
 */
public enum MembershipStatus {
    ACTIVE,
    SUSPENDED,
    REMOVED
}

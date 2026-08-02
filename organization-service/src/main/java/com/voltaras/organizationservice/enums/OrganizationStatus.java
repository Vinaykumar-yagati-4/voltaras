package com.voltaras.organizationservice.enums;

/**
 * Lifecycle status of an organization.
 * <ul>
 *   <li>{@link #ACTIVE} — organization is operational; normal operations are allowed.</li>
 *   <li>{@link #INACTIVE} — organization is deactivated; restricted operations are rejected.</li>
 *   <li>{@link #SUSPENDED} — organization is suspended (by System ADMIN); restricted operations are rejected.</li>
 * </ul>
 */
public enum OrganizationStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}

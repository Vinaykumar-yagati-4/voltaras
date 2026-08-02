package com.voltaras.organizationservice.enums;

/**
 * Role of a user within an organization.
 * <ul>
 *   <li>{@link #OWNER} — creator of the organization; full control, can assign/remove ORGANIZATION_ADMIN.</li>
 *   <li>{@link #ORGANIZATION_ADMIN} — administrative role assignable only by the OWNER.</li>
 *   <li>{@link #MANAGER} — can manage buildings, blocks, floors, and units when authorized.</li>
 *   <li>{@link #MEMBER} — regular organization member.</li>
 *   <li>{@link #TENANT} — resident member (apartment/hostel context).</li>
 *   <li>{@link #STUDENT} — learner member (institution context).</li>
 *   <li>{@link #STAFF} — staff member (institution/commercial context).</li>
 * </ul>
 */
public enum MembershipRole {
    OWNER,
    ORGANIZATION_ADMIN,
    MANAGER,
    MEMBER,
    TENANT,
    STUDENT,
    STAFF
}

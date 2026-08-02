package com.voltaras.organizationservice.enums;

/**
 * Type of organization managed by the Organization Service.
 * <ul>
 *   <li>{@link #HOSTEL} — student accommodation, e.g. Organization → Building → Block → Floor → Room.</li>
 *   <li>{@link #INSTITUTION} — educational institution, e.g. Organization → Building → Block/Department → Floor → Classroom/Lab.</li>
 *   <li>{@link #APARTMENT} — residential complex, e.g. Organization → Building/Tower → Block → Floor → Flat.</li>
 *   <li>{@link #COMMERCIAL} — commercial premises, e.g. Organization → Building → Wing/Block → Floor → Office/Shop.</li>
 * </ul>
 */
public enum OrganizationType {
    HOSTEL,
    INSTITUTION,
    APARTMENT,
    COMMERCIAL
}

/**
 * JPA entities for the Organization Service.
 * <p>
 * Implemented entities:
 * <ul>
 *   <li>{@link com.voltaras.organizationservice.entity.Organization} — organization profile (owner = creator).</li>
 *   <li>{@link com.voltaras.organizationservice.entity.OrganizationMembership} — user membership in an organization.</li>
 *   <li>{@link com.voltaras.organizationservice.entity.OrganizationJoinRequest} — pending/approved/rejected/cancelled join requests.</li>
 *   <li>{@link com.voltaras.organizationservice.entity.Building} — building in an organization.</li>
 *   <li>{@link com.voltaras.organizationservice.entity.Block} — block/wing/department inside a building.</li>
 *   <li>{@link com.voltaras.organizationservice.entity.Floor} — floor inside a block.</li>
 *   <li>{@link com.voltaras.organizationservice.entity.Unit} — room/flat/classroom/lab/office/shop inside a floor.</li>
 * </ul>
 * Entities are never exposed directly to clients.
 */
package com.voltaras.organizationservice.entity;

package com.voltaras.organizationservice.enums;

/**
 * Status of a physical structure (building, block, or floor).
 * <ul>
 *   <li>{@link #ACTIVE} — structure is operational.</li>
 *   <li>{@link #INACTIVE} — structure is deactivated.</li>
 *   <li>{@link #MAINTENANCE} — structure is under maintenance.</li>
 * </ul>
 */
public enum StructureStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE
}

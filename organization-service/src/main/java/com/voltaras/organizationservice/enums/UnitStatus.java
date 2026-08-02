package com.voltaras.organizationservice.enums;

/**
 * Status of a unit within a floor.
 * <ul>
 *   <li>{@link #AVAILABLE} — unit is free and can be assigned/occupied.</li>
 *   <li>{@link #OCCUPIED} — unit is currently occupied.</li>
 *   <li>{@link #INACTIVE} — unit is deactivated.</li>
 *   <li>{@link #MAINTENANCE} — unit is under maintenance.</li>
 * </ul>
 */
public enum UnitStatus {
    AVAILABLE,
    OCCUPIED,
    INACTIVE,
    MAINTENANCE
}

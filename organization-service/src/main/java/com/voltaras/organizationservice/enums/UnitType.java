package com.voltaras.organizationservice.enums;

/**
 * Type of a unit within a floor.
 * <ul>
 *   <li>{@link #ROOM} — hostel/building room.</li>
 *   <li>{@link #FLAT} — apartment flat.</li>
 *   <li>{@link #CLASSROOM} — institution classroom.</li>
 *   <li>{@link #LAB} — institution laboratory.</li>
 *   <li>{@link #OFFICE} — commercial office.</li>
 *   <li>{@link #SHOP} — commercial shop.</li>
 *   <li>{@link #OTHER} — any other unit type.</li>
 * </ul>
 */
public enum UnitType {
    ROOM,
    FLAT,
    CLASSROOM,
    LAB,
    OFFICE,
    SHOP,
    OTHER
}

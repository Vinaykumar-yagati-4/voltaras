package com.voltaras.organizationservice.dto.request;

import com.voltaras.organizationservice.enums.UnitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUnitRequest {

    @Size(max = 150, message = "Unit name must not exceed 150 characters")
    private String unitName;

    @NotNull(message = "Unit type is required")
    private UnitType unitType;

    @PositiveOrZero(message = "Capacity cannot be negative")
    private Integer capacity;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

package com.voltaras.organizationservice.dto.request;

import com.voltaras.organizationservice.enums.UnitStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUnitStatusRequest {

    @NotNull(message = "Status is required")
    private UnitStatus status;
}

package com.voltaras.organizationservice.dto.response;

import com.voltaras.organizationservice.enums.UnitStatus;
import com.voltaras.organizationservice.enums.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponse {

    private Long id;
    private Long floorId;
    private Integer floorNumber;
    private String unitNumber;
    private String unitName;
    private UnitType unitType;
    private Integer capacity;
    private UnitStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

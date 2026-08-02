package com.voltaras.organizationservice.dto.response;

import com.voltaras.organizationservice.enums.StructureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingResponse {

    private Long id;
    private Long organizationId;
    private String name;
    private String code;
    private String description;
    private String address;
    private StructureStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

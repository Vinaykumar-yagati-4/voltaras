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
public class BlockResponse {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private String name;
    private String code;
    private String description;
    private StructureStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

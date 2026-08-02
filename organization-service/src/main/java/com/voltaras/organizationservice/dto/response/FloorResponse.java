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
public class FloorResponse {

    private Long id;
    private Long blockId;
    private String blockName;
    private Integer floorNumber;
    private String name;
    private String description;
    private StructureStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

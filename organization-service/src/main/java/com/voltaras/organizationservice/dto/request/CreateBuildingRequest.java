package com.voltaras.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuildingRequest {

    @NotBlank(message = "Building name is required")
    @Size(max = 150, message = "Building name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Building code is required")
    @Size(max = 50, message = "Building code must not exceed 50 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}

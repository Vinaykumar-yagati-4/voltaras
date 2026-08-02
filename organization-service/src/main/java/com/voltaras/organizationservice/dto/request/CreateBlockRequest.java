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
public class CreateBlockRequest {

    @NotBlank(message = "Block name is required")
    @Size(max = 150, message = "Block name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Block code is required")
    @Size(max = 50, message = "Block code must not exceed 50 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

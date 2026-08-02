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
public class RejectJoinRequest {

    @NotBlank(message = "Rejection remarks are required")
    @Size(max = 500, message = "Rejection remarks must not exceed 500 characters")
    private String remarks;
}

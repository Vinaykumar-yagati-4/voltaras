package com.voltaras.paymentservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Field-level validation detail inside {@link ErrorResponse}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ValidationErrorDetail", description = "Field-level validation error")
public class ValidationErrorDetail {

    @Schema(description = "Name of the invalid field", example = "amount")
    private String field;

    @Schema(description = "Validation message", example = "must be greater than 0")
    private String message;
}

package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.dto.request.PayBillRequest;
import com.voltaras.paymentservice.dto.response.ErrorResponse;
import com.voltaras.paymentservice.dto.response.PaymentResponse;
import com.voltaras.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bill payment APIs settled from the wallet balance.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Bill Payment APIs",
        description = "Pay VOLTARAS bills from the wallet balance. "
                + "No card or UPI data is ever accepted."
)
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bills/{billId}/payments")
    @Operation(
            summary = "Pay a bill from the wallet",
            description = """
                    Pays the given bill from the authenticated user's wallet
                    balance and notifies the Bill Service (PAID when the
                    outstanding amount is settled, PARTIALLY_PAID otherwise).

                    The Idempotency-Key header is required: replaying the same
                    key with the same payload returns the original payment,
                    while reusing it with a different payload returns
                    409 IDEMPOTENCY_CONFLICT.

                    The bill must belong to the user, must be UNPAID or
                    PARTIALLY_PAID, and the user must be an active member of
                    the organization. If the wallet balance is insufficient,
                    400 INSUFFICIENT_WALLET_BALANCE is returned.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Payment created (or idempotently replayed)",
                    content = @Content(
                            schema = @Schema(implementation = PaymentResponse.class)
                    ),
                    headers = @Header(
                            name = "Location",
                            description = "URL of the created payment"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error, malformed request, missing "
                            + "Idempotency-Key, unpayable bill, amount above the "
                            + "outstanding amount or INSUFFICIENT_WALLET_BALANCE",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller cannot access the bill or organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key reused with a different payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Bill Service failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PaymentResponse> payBillFromWallet(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId,
            @Parameter(
                    description = "Idempotency key. Reusing it returns the original payment; "
                            + "reusing it with different data returns 409.",
                    required = true,
                    example = "pay-bill-1-2026-08-08"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PayBillRequest request) {

        PaymentResponse response = paymentService.payBillFromWallet(
                authUserId, systemRole, billId, idempotencyKey, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location",
                        "/api/payments/" + response.getId())
                .body(response);
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(
            summary = "Get a payment by ID",
            description = "Returns the payment only when it belongs to the "
                    + "authenticated user or the caller is a system ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment retrieved",
                    content = @Content(
                            schema = @Schema(implementation = PaymentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller cannot access this payment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Payment ID", example = "1")
            @PathVariable Long paymentId) {

        PaymentResponse response = paymentService.getPaymentById(
                authUserId, systemRole, paymentId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/reference/{paymentReference}")
    @Operation(
            summary = "Get a payment by payment reference",
            description = "Returns the payment only when it belongs to the "
                    + "authenticated user or the caller is a system ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment retrieved",
                    content = @Content(
                            schema = @Schema(implementation = PaymentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PaymentResponse> getPaymentByReference(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Server-generated payment reference", example = "PAY-3F9A2C7D...")
            @PathVariable String paymentReference) {

        PaymentResponse response = paymentService.getPaymentByReference(
                authUserId, systemRole, paymentReference);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bills/{billId}/payments")
    @Operation(
            summary = "List payments for a bill",
            description = "Returns all wallet payments for the bill, newest "
                    + "first. The caller must be the bill owner or a system "
                    + "ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payments retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = PaymentResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller cannot access this bill",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<PaymentResponse>> getPaymentsForBill(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId) {

        List<PaymentResponse> response = paymentService.getPaymentsForBill(
                authUserId, systemRole, billId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments")
    @Operation(
            summary = "List payments (paginated)",
            description = "Consumers get their own payments. System ADMINs may "
                    + "optionally filter by organizationId to list that "
                    + "organization's payments."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payments retrieved",
                    content = @Content(
                            schema = @Schema(implementation = Page.class)
                    )
            )
    })
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Optional organization filter (ADMIN only)", example = "6")
            @RequestParam(required = false) Long organizationId,
            @ParameterObject
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        Page<PaymentResponse> response = paymentService.getMyPayments(
                authUserId, systemRole, organizationId, pageable);

        return ResponseEntity.ok(response);
    }
}

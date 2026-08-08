package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.dto.request.CreateRechargeOrderRequest;
import com.voltaras.paymentservice.dto.response.ErrorResponse;
import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;
import com.voltaras.paymentservice.service.RechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wallet recharge APIs backed by the Razorpay gateway (sandbox mode).
 */
@RestController
@RequestMapping("/api/recharges")
@RequiredArgsConstructor
@Tag(
        name = "Wallet Recharge APIs",
        description = "Recharge the VOLTARAS wallet with UPI or CARD through "
                + "the Razorpay gateway in sandbox/test mode."
)
@SecurityRequirement(name = "bearerAuth")
public class RechargeController {

    private final RechargeService rechargeService;

    @PostMapping("/orders")
    @Operation(
            summary = "Create a recharge order",
            description = """
                    Creates a payment order at the Razorpay gateway for the
                    given amount. The frontend uses the returned order ID and
                    the public Razorpay key ID to run the checkout in sandbox
                    mode; the wallet is credited only when the gateway confirms
                    the payment through the webhook.

                    The Idempotency-Key header is required: replaying the same
                    key with the same payload returns the original order, while
                    reusing it with a different payload returns
                    409 IDEMPOTENCY_CONFLICT.

                    The user must be an active member of the organization.
                    No card numbers, CVVs or UPI PINs are ever accepted.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Recharge order details",
            required = true,
            content = @Content(
                    examples = {
                            @ExampleObject(
                                    name = "UPI recharge",
                                    summary = "Recharge 500 INR via UPI",
                                    value = """
                                            {
                                              "organizationId": 6,
                                              "amount": 500.00,
                                              "currency": "INR",
                                              "paymentMethod": "UPI"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Card recharge",
                                    summary = "Recharge 500 INR via card",
                                    value = """
                                            {
                                              "organizationId": 6,
                                              "amount": 500.00,
                                              "currency": "INR",
                                              "paymentMethod": "CARD"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Recharge order created (or idempotently replayed)",
                    content = @Content(
                            schema = @Schema(implementation = RechargeOrderResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error, malformed request, missing "
                            + "Idempotency-Key or invalid payment method",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an active member of the organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key reused with a different payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Razorpay gateway failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<RechargeOrderResponse> createRechargeOrder(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(
                    description = "Idempotency key. Reusing it returns the original order; "
                            + "reusing it with different data returns 409.",
                    required = true,
                    example = "recharge-500-upi-2026-08-08-01"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRechargeOrderRequest request) {

        RechargeOrderResponse response = rechargeService.createRechargeOrder(
                authUserId, systemRole, idempotencyKey, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my recharge history",
            description = "Returns the recharge transactions of the "
                    + "authenticated user, newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recharge history retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = RechargeTransactionResponse.class)
                            )
                    )
            )
    })
    public ResponseEntity<List<RechargeTransactionResponse>> getMyRecharges(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole) {

        List<RechargeTransactionResponse> response =
                rechargeService.getMyRecharges(authUserId, systemRole);

        return ResponseEntity.ok(response);
    }
}

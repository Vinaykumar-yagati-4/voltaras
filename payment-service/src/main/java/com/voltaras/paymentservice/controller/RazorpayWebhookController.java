package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.dto.response.ErrorResponse;
import com.voltaras.paymentservice.service.RechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Razorpay webhook callbacks for wallet recharges.
 *
 * <p>
 * The endpoint is public (the API Gateway whitelists
 * {@code /api/payments/webhooks/**}); protection is provided by the
 * HMAC-SHA256 signature in the {@code X-Razorpay-Signature} header,
 * verified against RAZORPAY_WEBHOOK_SECRET. Replayed callbacks are
 * idempotent and never credit the wallet twice.
 * </p>
 */
@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
@Tag(
        name = "Razorpay Webhook",
        description = "Gateway callbacks. Signature-protected with "
                + "RAZORPAY_WEBHOOK_SECRET; replayed events are idempotent."
)
@io.swagger.v3.oas.annotations.security.SecurityRequirements
public class RazorpayWebhookController {

    private final RechargeService rechargeService;

    @PostMapping("/razorpay")
    @Operation(
            summary = "Razorpay webhook",
            description = """
                    Receives Razorpay events (payment.captured, payment.failed,
                    payment.authorized, order.paid). The signature is verified
                    with HMAC-SHA256 over the raw body using
                    RAZORPAY_WEBHOOK_SECRET. A captured/paid event credits the
                    wallet; repeated callbacks are idempotent and return 200.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Webhook accepted (idempotent)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed webhook payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Invalid or missing webhook signature",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> handleRazorpayWebhook(
            @Parameter(
                    description = "Raw webhook payload; the HMAC-SHA256 "
                            + "signature is computed over exactly these bytes",
                    required = true
            )
            @RequestBody String payload,
            @Parameter(
                    description = "HMAC-SHA256 signature from Razorpay",
                    required = true,
                    example = "4f9a2c..."
            )
            @RequestHeader(value = "X-Razorpay-Signature",
                    required = false) String signature) {

        rechargeService.handleRazorpayWebhook(payload, signature);

        return ResponseEntity.ok().build();
    }
}

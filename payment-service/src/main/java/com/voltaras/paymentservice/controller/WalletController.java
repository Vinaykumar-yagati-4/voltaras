package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.service.RechargeService;
import com.voltaras.paymentservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(
        name = "Wallet APIs",
        description = "VOLTARAS prepaid wallet: balance, recharge, bill payment."
)
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final RechargeService rechargeService;

    @GetMapping("/me")
    @Operation(
            summary = "Get my wallet balance",
            description = "Returns the current wallet balance of the authenticated user. "
                    + "The wallet is created with a zero balance on first access."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet retrieved",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            )
    })
    public ResponseEntity<WalletResponse> getMyWallet(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "4")
            @RequestHeader("X-User-Id") Long authUserId,

            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole
    ) {

        WalletResponse response = walletService.getMyWallet(authUserId, systemRole);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/top-up")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Top up my wallet for local testing",
            description = """
                    Adds money to the authenticated user's wallet for
                    Swagger/local testing. No payment gateway is involved.

                    When an organizationId is provided, a local recharge
                    transaction (provider LOCAL, status SUCCESS) is persisted
                    so the recharge history reflects the credit; the caller
                    must be an active member of that organization. Without an
                    organizationId the wallet is credited without a recharge
                    record.
                    """
    )
    public ResponseEntity<WalletResponse> topUpWalletForTesting(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "4")
            @RequestHeader("X-User-Id") Long authUserId,

            @Parameter(description = "Role injected by the API Gateway", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,

            @RequestBody WalletTopUpRequest request
    ) {

        if (request == null || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Top-up amount must be greater than 0");
        }

        walletService.getMyWallet(authUserId, systemRole);

        if (request.organizationId() != null) {
            rechargeService.recordLocalRecharge(
                    authUserId, systemRole,
                    request.organizationId(), request.amount());
        } else {
            walletService.credit(authUserId, request.amount());
        }

        WalletResponse response = walletService.getMyWallet(authUserId, systemRole);

        return ResponseEntity.ok(response);
    }

    public record WalletTopUpRequest(
            @Schema(example = "500.00")
            BigDecimal amount,

            @Schema(
                    description = "Organization the user must actively belong to; "
                            + "when provided the top-up is also persisted as a "
                            + "local recharge transaction",
                    example = "6"
            )
            Long organizationId
    ) {
    }
}
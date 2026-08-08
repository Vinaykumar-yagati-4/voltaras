package com.voltaras.paymentservice.controller;

import com.voltaras.paymentservice.dto.response.WalletResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet APIs of the authenticated user.
 */
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

    @GetMapping("/me")
    @Operation(
            summary = "Get my wallet balance",
            description = "Returns the current wallet balance of the "
                    + "authenticated user. The wallet is created with a zero "
                    + "balance on first access."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet retrieved",
                    content = @Content(
                            schema = @Schema(implementation = WalletResponse.class)
                    )
            )
    })
    public ResponseEntity<WalletResponse> getMyWallet(
            @Parameter(description = "Authenticated user ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "CONSUMER")
            @RequestHeader("X-User-Role") String systemRole) {

        WalletResponse response = walletService.getMyWallet(
                authUserId, systemRole);

        return ResponseEntity.ok(response);
    }
}

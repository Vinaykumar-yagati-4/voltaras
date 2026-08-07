package com.voltaras.billservice.controller;

import com.voltaras.billservice.dto.request.CancelBillRequest;
import com.voltaras.billservice.dto.request.GenerateBillRequest;
import com.voltaras.billservice.dto.request.UpdateBillRequest;
import com.voltaras.billservice.dto.request.UpdatePaymentStatusRequest;
import com.voltaras.billservice.dto.response.BillResponse;
import com.voltaras.billservice.dto.response.BillSummaryResponse;
import com.voltaras.billservice.dto.response.ErrorResponse;
import com.voltaras.billservice.enums.BillStatus;
import com.voltaras.billservice.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(
        name = "Bill APIs",
        description = "Electricity bill generation, consumer history and admin management"
)
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;

    @GetMapping("/me")
    @Operation(
            summary = "Get my bills",
            description = "Returns the full bill history of the authenticated consumer, newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill history retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = BillSummaryResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "X-User-Id header missing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<BillSummaryResponse>> getMyBills(
            @Parameter(description = "Authenticated consumer ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        List<BillSummaryResponse> response =
                billService.getMyBills(authUserId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/{billId}")
    @Operation(
            summary = "Get my bill by ID",
            description = "Returns a single bill only when it belongs to the authenticated consumer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill retrieved",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found for this consumer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> getMyBillById(
            @Parameter(description = "Authenticated consumer ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId) {

        BillResponse response =
                billService.getMyBillById(authUserId, billId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/filter")
    @Operation(
            summary = "Get my bills by billing period",
            description = "Returns the consumer's bills filtered by billing month and year."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bills retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = BillSummaryResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid month or year",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<BillSummaryResponse>> getMyBillsByPeriod(
            @Parameter(description = "Authenticated consumer ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId,
            @Parameter(description = "Billing month (1-12)", example = "8")
            @RequestParam Integer month,
            @Parameter(description = "Billing year", example = "2026")
            @RequestParam Integer year) {

        List<BillSummaryResponse> response =
                billService.getMyBillsByPeriod(authUserId, month, year);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/outstanding")
    @Operation(
            summary = "Get my outstanding bills",
            description = "Returns bills that are still payable (UNPAID, PARTIALLY_PAID or FAILED, not CANCELLED)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Outstanding bills retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = BillSummaryResponse.class)
                            )
                    )
            )
    })
    public ResponseEntity<List<BillSummaryResponse>> getMyOutstandingBills(
            @Parameter(description = "Authenticated consumer ID injected by the API Gateway", example = "13")
            @RequestHeader("X-User-Id") Long authUserId) {

        List<BillSummaryResponse> response =
                billService.getMyOutstandingBills(authUserId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin")
    @Operation(
            summary = "Generate a bill",
            description = "Creates a new bill for the consumer identified by authUserId in the request body. "
                    + "The authenticated Admin is recorded separately as generatedBy. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Bill generated",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid consumer ID, readings, billing period, due date, or missing headers",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate bill for the same consumer, meter, month and year",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> generateBill(
            @Parameter(description = "Authenticated Admin ID injected by the API Gateway", example = "4")
            @RequestHeader("X-User-Id") Long adminUserId,
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Valid @RequestBody GenerateBillRequest request) {

        BillResponse response =
                billService.generateBill(adminUserId, systemRole, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin")
    @Operation(
            summary = "Get all bills with optional filters",
            description = "Returns all bills, optionally filtered by status, billing month and billing year. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bills retrieved",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(implementation = BillSummaryResponse.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid status or month filter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<BillSummaryResponse>> getAllBills(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Optional bill status filter", example = "GENERATED")
            @RequestParam(required = false) BillStatus status,
            @Parameter(description = "Optional billing month filter (1-12)", example = "8")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Optional billing year filter", example = "2026")
            @RequestParam(required = false) Integer year) {

        List<BillSummaryResponse> response =
                billService.getAllBills(systemRole, status, month, year);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{billId}")
    @Operation(
            summary = "Get bill by ID (Admin)",
            description = "Returns full details of any bill. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill retrieved",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> getBillById(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId) {

        BillResponse response =
                billService.getBillById(systemRole, billId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/{billId}")
    @Operation(
            summary = "Update a bill",
            description = "Updates the due date, late fee and remarks, then recalculates the total amount. "
                    + "PAID and CANCELLED bills cannot be modified. Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill updated",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Business rule violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> updateBill(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId,
            @Valid @RequestBody UpdateBillRequest request) {

        BillResponse response =
                billService.updateBill(systemRole, billId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{billId}/cancel")
    @Operation(
            summary = "Cancel a bill",
            description = "Cancels a bill with a reason. PAID bills cannot be cancelled. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bill cancelled",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "PAID or already-cancelled bill",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> cancelBill(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId,
            @Valid @RequestBody CancelBillRequest request) {

        BillResponse response =
                billService.cancelBill(systemRole, billId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{billId}/payment-status")
    @Operation(
            summary = "Update payment status",
            description = "Updates the payment state of a bill. CANCELLED bills cannot be paid later. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment status updated",
                    content = @Content(schema = @Schema(implementation = BillResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Business rule violation or invalid enum value",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bill not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BillResponse> updatePaymentStatus(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole,
            @Parameter(description = "Bill ID", example = "1")
            @PathVariable Long billId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {

        BillResponse response =
                billService.updatePaymentStatus(systemRole, billId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/mark-overdue")
    @Operation(
            summary = "Mark overdue bills",
            description = "Marks all payable bills whose due date has passed as OVERDUE and applies "
                    + "the ₹50.00 late fee. Returns the number of bills updated. "
                    + "Requires X-User-Role = ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Number of bills marked overdue",
                    content = @Content(schema = @Schema(example = "3"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Integer> markOverdue(
            @Parameter(description = "Role injected by the API Gateway (ADMIN or ROLE_ADMIN)", example = "ADMIN")
            @RequestHeader("X-User-Role") String systemRole) {

        int updated = billService.markOverdue(systemRole);

        return ResponseEntity.ok(updated);
    }
}
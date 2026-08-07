package com.voltaras.billservice.mapper;

import com.voltaras.billservice.dto.request.GenerateBillRequest;
import com.voltaras.billservice.dto.request.UpdateBillRequest;
import com.voltaras.billservice.dto.response.BillResponse;
import com.voltaras.billservice.dto.response.BillSummaryResponse;
import com.voltaras.billservice.entity.Bill;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Maps {@link Bill} entities to response DTOs.
 *
 * <p>{@link GenerateBillRequest} is deliberately not mapped directly to the
 * entity. Every calculated field, including units consumed, energy charge,
 * tax, total, and outstanding amount, is assigned by the service layer.</p>
 *
 * <p>System-controlled fields such as the consumer's auth user ID, bill
 * statuses, generating administrator, and timestamps are also assigned by
 * the service layer.</p>
 *
 * <p>Updates use {@link NullValuePropertyMappingStrategy#IGNORE}, so null
 * fields in {@link UpdateBillRequest} leave the existing entity values
 * unchanged. Monetary recalculation occurs in the service layer after
 * mapping.</p>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE
)
public interface BillMapper {

    BillResponse toResponse(Bill bill);

    BillSummaryResponse toSummary(Bill bill);

    void updateBill(
            UpdateBillRequest request,
            @MappingTarget Bill bill
    );
}
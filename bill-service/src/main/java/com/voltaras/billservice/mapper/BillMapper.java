package com.voltaras.billservice.mapper;

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
 * <p>
 * {@link GenerateBillRequest} is deliberately not mapped to the entity:
 * every calculated field (units, energy charge, tax, total, outstanding)
 * and system-controlled field (authUserId, statuses, generatedBy,
 * timestamps) is assigned by the service layer to keep the calculation
 * rules in one place.
 * </p>
 *
 * <p>
 * Updates use {@link NullValuePropertyMappingStrategy#IGNORE} so null
 * fields in {@link UpdateBillRequest} leave the existing entity values
 * untouched; monetary recalculation happens in the service layer after
 * mapping.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BillMapper {

    BillResponse toResponse(Bill bill);

    BillSummaryResponse toSummary(Bill bill);

    void updateBill(UpdateBillRequest request, @MappingTarget Bill bill);
}

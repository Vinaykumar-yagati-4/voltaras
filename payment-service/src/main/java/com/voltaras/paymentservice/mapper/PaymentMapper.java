package com.voltaras.paymentservice.mapper;

import com.voltaras.paymentservice.dto.response.PaymentResponse;
import com.voltaras.paymentservice.entity.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps {@link PaymentTransaction} entities to response DTOs.
 *
 * <p>Request DTOs are deliberately not mapped directly to the entity: the
 * payment reference, statuses and timestamps are all assigned by the
 * service layer.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentMapper {

    PaymentResponse toResponse(PaymentTransaction payment);
}

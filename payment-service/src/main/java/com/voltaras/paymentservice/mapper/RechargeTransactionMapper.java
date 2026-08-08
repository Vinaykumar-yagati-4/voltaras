package com.voltaras.paymentservice.mapper;

import com.voltaras.paymentservice.dto.response.RechargeOrderResponse;
import com.voltaras.paymentservice.dto.response.RechargeTransactionResponse;
import com.voltaras.paymentservice.entity.RechargeTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps {@link RechargeTransaction} entities to response DTOs.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RechargeTransactionMapper {

    /**
     * Maps to the order-creation response. The Razorpay key ID is filled
     * by the service layer after mapping.
     */
    RechargeOrderResponse toOrderResponse(RechargeTransaction recharge);

    /**
     * Maps to the history response.
     */
    RechargeTransactionResponse toTransactionResponse(
            RechargeTransaction recharge);
}

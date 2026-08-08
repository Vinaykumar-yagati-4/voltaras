package com.voltaras.paymentservice.mapper;

import com.voltaras.paymentservice.dto.response.WalletResponse;
import com.voltaras.paymentservice.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps {@link Wallet} entities to response DTOs.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WalletMapper {

    WalletResponse toResponse(Wallet wallet);
}

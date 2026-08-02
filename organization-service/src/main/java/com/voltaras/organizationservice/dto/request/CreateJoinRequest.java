package com.voltaras.organizationservice.dto.request;

import com.voltaras.organizationservice.enums.MembershipRole;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJoinRequest {

    private MembershipRole requestedRole;

    @Size(max = 500, message = "Request message must not exceed 500 characters")
    private String requestMessage;
}

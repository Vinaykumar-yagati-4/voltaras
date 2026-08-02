package com.voltaras.organizationservice.dto.request;

import com.voltaras.organizationservice.enums.MembershipRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMembershipRoleRequest {

    @NotNull(message = "Role is required")
    private MembershipRole role;
}

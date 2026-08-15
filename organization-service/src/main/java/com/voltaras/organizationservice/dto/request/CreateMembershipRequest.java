package com.voltaras.organizationservice.dto.request;

import com.voltaras.organizationservice.enums.MembershipRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used by system ADMINs to create (or reactivate) an ACTIVE
 * organization membership for a user. Only MEMBER and MANAGER roles may
 * be assigned; OWNER and ORGANIZATION_ADMIN are reserved for the
 * organization workflow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMembershipRequest {

    @NotNull(message = "authUserId is required")
    @Positive(message = "authUserId must be positive")
    private Long authUserId;

    private MembershipRole membershipRole;
}

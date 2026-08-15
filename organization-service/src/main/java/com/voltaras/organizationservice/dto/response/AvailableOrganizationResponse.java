package com.voltaras.organizationservice.dto.response;

import com.voltaras.organizationservice.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight, consumer-facing view of an organization that is available
 * to request access to. Contact details are intentionally excluded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableOrganizationResponse {

    private Long id;
    private String name;
    private String organizationCode;
    private OrganizationType organizationType;
    private String description;
    private String city;
    private String state;
    private String country;
}

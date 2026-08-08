package com.voltaras.paymentservice.client.impl;

import com.voltaras.paymentservice.client.OrganizationServiceClient;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.ResourceNotFoundException;
import com.voltaras.paymentservice.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link OrganizationServiceClient} using Spring's
 * {@link RestClient}. It reuses the Organization Service's existing
 * {@code GET /api/organizations/{id}} endpoint, which already enforces
 * ACTIVE membership (any role) or system ADMIN access.
 */
@Component
public class RestOrganizationServiceClient implements OrganizationServiceClient {

    private final RestClient restClient;

    public RestOrganizationServiceClient(
            @Value("${app.organization-service.base-url}") String baseUrl) {

        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void requireOrganizationAccess(
            Long organizationId, Long authUserId, String systemRole) {

        try {

            restClient.get()
                    .uri("/api/organizations/{organizationId}", organizationId)
                    .header("X-User-Id", String.valueOf(authUserId))
                    .header("X-User-Role",
                            systemRole != null ? systemRole : "")
                    .retrieve()
                    .onStatus(status -> status.value() == 403, (request, response) -> {
                        throw new ForbiddenOperationException(
                                "You are not an active member of this organization");
                    })
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException(
                                "Organization", "id", organizationId);
                    })
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {

            throw new UpstreamServiceException(
                    "Failed to verify access to organization " + organizationId, ex);
        }
    }
}

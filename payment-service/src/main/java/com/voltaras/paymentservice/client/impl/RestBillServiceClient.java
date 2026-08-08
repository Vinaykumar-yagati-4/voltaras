package com.voltaras.paymentservice.client.impl;

import com.voltaras.paymentservice.client.BillServiceClient;
import com.voltaras.paymentservice.client.BillSnapshot;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import com.voltaras.paymentservice.exception.ResourceNotFoundException;
import com.voltaras.paymentservice.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * HTTP implementation of {@link BillServiceClient} using Spring's
 * {@link RestClient}.
 *
 * <p>
 * Identity is propagated using the same X-User-Id / X-User-Role header
 * convention the API Gateway uses for browser requests. This is the
 * repository's only identity-propagation mechanism, so inter-service calls
 * reuse it.
 * </p>
 */
@Component
public class RestBillServiceClient implements BillServiceClient {

    private final RestClient restClient;

    public RestBillServiceClient(
            @Value("${app.bill-service.base-url}") String baseUrl) {

        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public BillSnapshot getConsumerBill(Long billId, Long authUserId) {

        try {

            return restClient.get()
                    .uri("/api/bills/me/{billId}", billId)
                    .header("X-User-Id", String.valueOf(authUserId))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("Bill", "id", billId);
                    })
                    .onStatus(status -> status.value() == 403, (request, response) -> {
                        throw new ForbiddenOperationException(
                                "You are not allowed to access this bill");
                    })
                    .body(BillSnapshot.class);

        } catch (RestClientResponseException ex) {

            throw new UpstreamServiceException(
                    "Bill Service failed to return bill " + billId, ex);
        }
    }

    @Override
    public BillSnapshot getBillAsAdmin(Long billId, String systemRole) {

        try {

            return restClient.get()
                    .uri("/api/bills/admin/{billId}", billId)
                    .header("X-User-Role",
                            systemRole != null ? systemRole : "")
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("Bill", "id", billId);
                    })
                    .onStatus(status -> status.value() == 403, (request, response) -> {
                        throw new ForbiddenOperationException(
                                "System ADMIN role is required to access this bill");
                    })
                    .body(BillSnapshot.class);

        } catch (RestClientResponseException ex) {

            throw new UpstreamServiceException(
                    "Bill Service failed to return bill " + billId, ex);
        }
    }

    @Override
    public void notifyPaymentStatus(
            Long billId, String paymentStatus, BigDecimal cumulativeAmountPaid) {

        try {

            String payload = """
                    {
                      "paymentStatus": "%s",
                      "amountPaid": %s
                    }
                    """.formatted(paymentStatus, cumulativeAmountPaid.toPlainString());

            restClient.patch()
                    .uri("/api/bills/admin/{billId}/payment-status", billId)
                    .header("X-User-Role", "ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {

            throw new UpstreamServiceException(
                    "Failed to notify Bill Service about payment for bill " + billId, ex);
        }
    }
}

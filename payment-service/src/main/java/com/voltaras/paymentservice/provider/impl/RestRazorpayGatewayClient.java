package com.voltaras.paymentservice.provider.impl;

import com.voltaras.paymentservice.exception.PaymentProviderException;
import com.voltaras.paymentservice.provider.RazorpayCreateOrderRequest;
import com.voltaras.paymentservice.provider.RazorpayGatewayClient;
import com.voltaras.paymentservice.provider.RazorpayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * HTTP implementation of {@link RazorpayGatewayClient} using Spring's
 * {@link RestClient} in sandbox/test mode.
 *
 * <p>
 * The gateway is authenticated with the key ID and key secret using HTTP
 * Basic auth (same mechanism Razorpay documents for server-side calls).
 * Amounts are passed in paise. Webhook signatures are verified with
 * HMAC-SHA256 over the raw payload using the shared webhook secret.
 * </p>
 */
@Component
@Slf4j
public class RestRazorpayGatewayClient implements RazorpayGatewayClient {

    private final RestClient restClient;

    public RestRazorpayGatewayClient(
            @Value("${app.razorpay.base-url}") String baseUrl,
            @Value("${app.razorpay.key-id}") String keyId,
            @Value("${app.razorpay.key-secret}") String keySecret) {

        String credentials = Base64.getEncoder().encodeToString(
                (keyId + ":" + keySecret)
                        .getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .build();
    }

    @Override
    public RazorpayOrder createOrder(RazorpayCreateOrderRequest request) {

        try {

            return restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RazorpayOrder.class);

        } catch (RestClientResponseException ex) {

            log.error("Razorpay createOrder failed with status {}: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            throw new PaymentProviderException(
                    "Razorpay gateway failed to create the order", ex);
        }
    }

    @Override
    public boolean verifyWebhookSignature(
            String payload, String signature, String secret) {

        if (payload == null || signature == null || signature.isBlank()
                || secret == null || secret.isBlank()) {

            return false;
        }

        try {

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            byte[] expected = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8));

            String expectedHex = HexFormat.of().formatHex(expected);

            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase()
                            .getBytes(StandardCharsets.UTF_8));

        } catch (Exception ex) {

            log.warn("Webhook signature verification failed: {}", ex.getMessage());

            return false;
        }
    }
}

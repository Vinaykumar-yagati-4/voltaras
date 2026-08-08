package com.voltaras.paymentservice.provider.impl;

import com.voltaras.paymentservice.provider.RazorpayGatewayClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RestRazorpayGatewayClient} webhook signature
 * verification. The HMAC-SHA256 computation is deterministic, so known
 * vectors are computed locally and compared.
 */
class RazorpayGatewayClientTest {

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    private final RazorpayGatewayClient client =
            new RestRazorpayGatewayClient(
                    "http://localhost:9999",
                    "rzp_test_key",
                    "test-key-secret");

    @Test
    @DisplayName("A correctly computed signature is accepted")
    void validSignature_accepted() {

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"order_id":"order_1"}}}}
                """;

        String signature = hmacSha256Hex(payload, WEBHOOK_SECRET);

        assertThat(client.verifyWebhookSignature(
                payload, signature, WEBHOOK_SECRET)).isTrue();
    }

    @Test
    @DisplayName("A signature for a different payload is rejected")
    void signatureForOtherPayload_rejected() {

        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"order_id":"order_1"}}}}
                """;

        String otherSignature = hmacSha256Hex(
                "{\"event\":\"payment.failed\"}", WEBHOOK_SECRET);

        assertThat(client.verifyWebhookSignature(
                payload, otherSignature, WEBHOOK_SECRET)).isFalse();
    }

    @Test
    @DisplayName("A signature computed with a different secret is rejected")
    void signatureWithOtherSecret_rejected() {

        String payload = """
                {"event":"payment.captured"}
                """;

        String signature = hmacSha256Hex(payload, "another-secret");

        assertThat(client.verifyWebhookSignature(
                payload, signature, WEBHOOK_SECRET)).isFalse();
    }

    @Test
    @DisplayName("Missing signature or secret is rejected")
    void missingSignatureOrSecret_rejected() {

        String payload = "{}";

        assertThat(client.verifyWebhookSignature(
                payload, null, WEBHOOK_SECRET)).isFalse();

        assertThat(client.verifyWebhookSignature(
                payload, "abc", null)).isFalse();

        assertThat(client.verifyWebhookSignature(
                null, "abc", WEBHOOK_SECRET)).isFalse();

        assertThat(client.verifyWebhookSignature(
                payload, "   ", WEBHOOK_SECRET)).isFalse();
    }

    private String hmacSha256Hex(String payload, String secret) {

        try {

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            return HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        } catch (Exception ex) {

            throw new IllegalStateException(ex);
        }
    }
}

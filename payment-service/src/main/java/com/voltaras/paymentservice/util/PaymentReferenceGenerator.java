package com.voltaras.paymentservice.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates unique, immutable payment references. References are always
 * server-generated and are never accepted from the client.
 */
@Component
public class PaymentReferenceGenerator {

    /**
     * Generates a payment reference such as {@code PAY-3F9A...}.
     */
    public String generate() {

        return generate("PAY");
    }

    /**
     * Generates a reference with the given prefix, for example
     * {@code RCH-...} for recharges.
     *
     * @param prefix reference prefix (uppercased)
     * @return reference such as {@code PREFIX-3F9A...}
     */
    public String generate(String prefix) {

        String uuid = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();

        return prefix.toUpperCase() + "-" + uuid;
    }
}

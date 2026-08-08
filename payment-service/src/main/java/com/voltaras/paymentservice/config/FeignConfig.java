package com.voltaras.paymentservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign configuration for the Payment Service.
 *
 * <p>
 * The API Gateway forwards the original {@code Authorization: Bearer ...}
 * header together with the X-User-Id / X-User-Role headers. This request
 * interceptor re-attaches that Bearer token to every Feign call, so the
 * Auth Service internal endpoint receives the same JWT the gateway
 * validated and always authenticates the correct user.
 * </p>
 */
@Configuration
public class FeignConfig {

    /**
     * Copies the {@code Authorization} header from the incoming servlet
     * request onto outgoing Feign requests. When the current request has
     * no token (for example the public Razorpay webhook, which performs
     * no Feign call anyway) the header is simply omitted.
     *
     * <p>
     * NOTE: this interceptor is registered globally and therefore applies
     * to every Feign client of this service. Today the service has exactly
     * one client ({@code AuthServiceClient}) and forwarding the user's
     * Bearer token is exactly what it needs. If a future Feign client is
     * added that must NOT receive the user's token, scope this interceptor
     * per client via {@code @FeignClient(configuration = ...)}.
     * </p>
     */
    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {

        return requestTemplate -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder
                            .getRequestAttributes();

            if (attributes == null) {
                return;
            }

            String authorization = attributes
                    .getRequest()
                    .getHeader(HttpHeaders.AUTHORIZATION);

            if (authorization != null && !authorization.isBlank()) {
                requestTemplate.header(
                        HttpHeaders.AUTHORIZATION,
                        authorization
                );
            }
        };
    }
}

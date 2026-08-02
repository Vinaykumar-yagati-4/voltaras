/**
 * Security-related classes for the Organization Service.
 * <p>
 * Reserved for any security configuration to be added during implementation.
 * Note: the Organization Service does <b>not</b> parse or validate JWTs —
 * identity comes exclusively from the {@code X-User-Id} / {@code X-User-Role}
 * headers injected by the API Gateway (docs/10 §2).
 */
package com.voltaras.organizationservice.security;

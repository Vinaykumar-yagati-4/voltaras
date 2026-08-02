/**
 * Security-related classes for the Organization Service.
 * <p>
 * The Organization Service does <b>not</b> parse or validate JWTs — identity
 * comes exclusively from the {@code X-User-Id} / {@code X-User-Role} headers
 * injected by the API Gateway (docs/10 §2). Spring Security permits all
 * requests ({@code SecurityConfig}); real authorization happens in the
 * service layer using {@link
 * com.voltaras.organizationservice.security.OrganizationAccessHelper}.
 */
package com.voltaras.organizationservice.security;

package com.voltaras.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * The main filter method — called for every incoming request.
     *
     * @param request     The incoming HTTP request
     * @param response    The HTTP response
     * @param filterChain The next filter in the chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Step 1: Extract the JWT from the Authorization header
            String token = extractTokenFromRequest(request);

            // Step 2: If token exists, is valid AND is an ACCESS token,
            // authenticate the user. Refresh tokens are never accepted
            // as Bearer credentials, so a stolen refresh token cannot be
            // used against protected endpoints after logout.
            if (StringUtils.hasText(token)
                    && jwtTokenProvider.validateAccessToken(token)) {

                // Step 3: Get the email from the token
                String email = jwtTokenProvider.getEmailFromToken(token);

                // Step 4: Load user details from the database
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                /*
                 * Step 4b: Attach the verified access token's secure
                 * session identifier to the principal so protected
                 * endpoints (e.g. logout) can revoke the matching
                 * refresh session from the security context.
                 */
                if (userDetails instanceof CustomUserDetails customUserDetails) {
                    customUserDetails.setSessionId(
                            jwtTokenProvider.getSessionIdFromToken(token)
                    );
                }

                // Step 5: Create an Authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Step 6: Set request details (IP, session ID, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 7: Set the authentication in the SecurityContext
                // This tells Spring Security: "This user is authenticated!"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        // Step 8: Continue the filter chain
        // Whether authenticated or not, the request continues.
        // The SecurityConfig will decide which endpoints require auth.
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix (7 characters)
            return bearerToken.substring(7);
        }

        return null;
    }
}

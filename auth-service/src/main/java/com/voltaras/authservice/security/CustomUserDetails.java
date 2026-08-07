package com.voltaras.authservice.security;

import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Secure session identifier (sid claim) of the verified access
     * token that authenticated this principal. Populated by
     * JwtAuthenticationFilter so protected endpoints (e.g. logout)
     * can revoke the matching refresh session.
     */
    private String sessionId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if (user.getUserRoles() == null
                || user.getUserRoles().isEmpty()) {
            return Set.of();
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {

        /*
         * Guard against NULL is_active values left behind by older
         * schema versions - a NULL Boolean would otherwise cause a
         * NullPointerException here and turn login into HTTP 500.
         * NULL is treated as an inactive account (DisabledException).
         */
        return Boolean.TRUE.equals(user.getIsActive());
    }
}

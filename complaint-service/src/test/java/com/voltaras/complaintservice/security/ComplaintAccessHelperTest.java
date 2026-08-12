package com.voltaras.complaintservice.security;

import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.exception.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the role/ownership helper. The gateway forwards the JWT
 * {@code role} claim verbatim and the Auth Service issues it as the
 * RoleType enum name, so the canonical values are exactly {@code ADMIN}
 * and {@code CONSUMER} — {@code ROLE_ADMIN} is not produced by the VOLTARAS
 * gateway and is intentionally rejected.
 */
class ComplaintAccessHelperTest {

    private final ComplaintAccessHelper helper = new ComplaintAccessHelper();

    @Test
    @DisplayName("Admin check accepts the exact gateway role ADMIN")
    void requireAdmin_acceptsExactAdminRole() {
        assertThatCode(() -> helper.requireAdmin("ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Admin check rejects the ROLE_ADMIN spelling (not the gateway format)")
    void requireAdmin_rejectsRoleAdminSpelling() {
        assertThatThrownBy(() -> helper.requireAdmin("ROLE_ADMIN"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    @DisplayName("Admin check rejects CONSUMER, null and blank roles")
    void requireAdmin_rejectsNonAdminRoles() {
        assertThatThrownBy(() -> helper.requireAdmin("CONSUMER"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> helper.requireAdmin((String) null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> helper.requireAdmin("  "))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Consumer check accepts the exact gateway role CONSUMER")
    void requireConsumer_acceptsExactConsumerRole() {
        assertThatCode(() -> helper.requireConsumer("CONSUMER"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Consumer check rejects ADMIN")
    void requireConsumer_rejectsAdmin() {
        assertThatThrownBy(() -> helper.requireConsumer("ADMIN"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Owner check accepts the complaint owner")
    void requireOwner_acceptsOwner() {
        Complaint complaint = Complaint.builder().consumerId(13L).build();

        assertThatCode(() -> helper.requireOwner(complaint, 13L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Owner check rejects a different user (admins cannot own complaints)")
    void requireOwner_rejectsForeignUser() {
        Complaint complaint = Complaint.builder().consumerId(13L).build();

        assertThatThrownBy(() -> helper.requireOwner(complaint, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}

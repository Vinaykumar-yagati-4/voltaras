package com.voltaras.authservice.service.impl;

import com.voltaras.authservice.service.PasswordResetMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development-only mail sender. Logs the reset link (including the
 * raw token) so the flow can be tested locally without an SMTP server.
 *
 * Only active when {@code app.password-reset.mail.provider=console}
 * (development or test environments). In production the default
 * {@code smtp} implementation is used and the raw token is never
 * written to logs.
 */
@Component
@ConditionalOnProperty(
        name = "app.password-reset.mail.provider",
        havingValue = "console",
        matchIfMissing = false
)
public class ConsolePasswordResetMailService
        implements PasswordResetMailService {

    private static final Logger log =
            LoggerFactory.getLogger(ConsolePasswordResetMailService.class);

    @Override
    public void sendPasswordResetEmail(
            String recipientEmail,
            String resetLink
    ) {

        log.info(
                """
                ============================================================
                [DEV MAIL - NOT SENT] Password reset for {}
                Reset link: {}
                ============================================================
                """,
                recipientEmail,
                resetLink
        );
    }
}

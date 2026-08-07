package com.voltaras.authservice.service.impl;

import com.voltaras.authservice.service.PasswordResetMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Production mail sender backed by Spring Boot Mail (JavaMailSender).
 * Credentials come exclusively from environment variables via
 * {@code spring.mail.*} - nothing is hardcoded.
 */
@Component
@ConditionalOnProperty(
        name = "app.password-reset.mail.provider",
        havingValue = "smtp",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class SmtpPasswordResetMailService
        implements PasswordResetMailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.mail.from:noreply@voltaras.com}")
    private String fromAddress;

    @Value("${app.password-reset.expiration-minutes:15}")
    private int expirationMinutes;

    @Override
    public void sendPasswordResetEmail(
            String recipientEmail,
            String resetLink
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("VOLTARAS - Reset your password");
        message.setText(
                """
                Hello,

                We received a request to reset the password for your
                VOLTARAS account.

                Click the link below to choose a new password. The link
                is valid for %d minutes and can only be used once:

                %s

                If you did not request a password reset, you can safely
                ignore this email.

                Regards,
                VOLTARAS Team
                """.formatted(expirationMinutes, resetLink)
        );

        mailSender.send(message);
    }
}

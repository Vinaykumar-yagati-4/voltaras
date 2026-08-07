package com.voltaras.authservice.service;

/**
 * Abstraction over how password-reset emails are delivered.
 *
 * Two implementations exist:
 * <ul>
 *   <li>{@code console} - logs the reset link (development / tests)</li>
 *   <li>{@code smtp} - sends a real email (default / production)</li>
 * </ul>
 * The active implementation is selected with
 * {@code app.password-reset.mail.provider}.
 */
public interface PasswordResetMailService {

    /**
     * Delivers the password-reset email containing the reset link.
     * The link embeds the raw one-time token.
     */
    void sendPasswordResetEmail(String recipientEmail, String resetLink);
}

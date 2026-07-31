package com.voltaras.userservice.exception;

/**
 * Thrown when a requested resource (e.g. a user profile) cannot be found.
 * <p>
 * Message format follows the auth-service convention:
 * {@code "UserProfile not found with authUserId: '5'"}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}

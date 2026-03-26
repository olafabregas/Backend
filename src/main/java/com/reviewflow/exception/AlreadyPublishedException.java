package com.reviewflow.exception;

/**
 * AlreadyPublishedException — thrown when attempting to publish an
 * already-published announcement. HTTP 409 Conflict.
 */
public class AlreadyPublishedException extends BusinessRuleException {

    public AlreadyPublishedException(String message) {
        super(message, "ALREADY_PUBLISHED");
    }

    public AlreadyPublishedException() {
        super("This announcement is already published", "ALREADY_PUBLISHED");
    }
}

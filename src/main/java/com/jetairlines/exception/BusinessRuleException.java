package com.jetairlines.exception;

/** A valid request that breaks a business rule. Maps to HTTP 409 (REST) / BAD_REQUEST (GraphQL). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}

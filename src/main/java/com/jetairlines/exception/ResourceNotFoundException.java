package com.jetairlines.exception;

/** Maps to HTTP 404 (REST) / NOT_FOUND (GraphQL). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

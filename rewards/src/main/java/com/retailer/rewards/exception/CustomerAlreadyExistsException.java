package com.retailer.rewards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when attempting to create a customer
 * with an ID that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT) // Maps this exception to a 409 Conflict HTTP status
public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException(String customerId) {
        super("Customer with ID '" + customerId + "' already exists.");
    }
}
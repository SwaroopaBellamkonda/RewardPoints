package com.retailer.rewards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a requested customer is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Maps this exception to a 404 Not Found HTTP status
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(String customerId, String period) {
        super("Customer with ID '" + customerId + "' not found or has no transactions for the period: " + period);
    }
}
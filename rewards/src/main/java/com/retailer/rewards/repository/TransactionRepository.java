package com.retailer.rewards.repository;

import com.retailer.rewards.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Transaction entities.
 * Provides CRUD operations and custom queries for Transaction.
 */

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds all transactions within a specified date range.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return A list of transactions within the given date range.
     */
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds all transactions for a given customer within a specified date range.
     * @param customerId The business ID of the customer.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return A list of transactions for the specified customer within the given date range.
     */
    List<Transaction> findByCustomer_CustomerIdAndTransactionDateBetween(String customerId, LocalDate startDate, LocalDate endDate);

    /**
     * Finds all transactions for a given customer.
     * @param customerId The business ID of the customer.
     * @return A list of all transactions for the specified customer.
     */
    List<Transaction> findByCustomer_CustomerId(String customerId);
}

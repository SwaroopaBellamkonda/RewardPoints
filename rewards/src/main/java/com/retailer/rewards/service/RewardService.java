package com.retailer.rewards.service;

import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException; // Import the new exception
import com.retailer.rewards.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class to calculate reward points for customers.
 * Now interacts with the TransactionRepository to fetch data.
 */

@Service
public class RewardService {

    private static final int POINTS_OVER_100_THRESHOLD = 100;
    private static final int POINTS_BETWEEN_50_AND_100_THRESHOLD = 50;
    private static final int POINTS_RATE_OVER_100 = 2;
    private static final int POINTS_RATE_BETWEEN_50_AND_100 = 1;

    private final TransactionRepository transactionRepository;

    @Autowired
    public RewardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Calculates the reward points for a given transaction amount.
     * Rules:
     *  1 point for every dollar spent between $50 and $100.
     *  2 points for every dollar spent over $100.
     *
     * @param amount The transaction amount.
     * @return The calculated reward points for the transaction.
     */
    public int calculatePoints(double amount){

        int points = 0;

        // Ensure amount is treated as an integer for dollar calculation
        int dollars = (int) Math.floor(amount);

        // Points for dollars over $100
        if (dollars > POINTS_OVER_100_THRESHOLD) {
            points += (dollars - POINTS_OVER_100_THRESHOLD) * POINTS_RATE_OVER_100;
        }

        // Points for dollars between $50 and $100
        if (dollars > POINTS_BETWEEN_50_AND_100_THRESHOLD) {
            // Calculate dollars strictly between 50 and 100
            int dollarsBetween50And100 = Math.min(dollars, POINTS_OVER_100_THRESHOLD) - POINTS_BETWEEN_50_AND_100_THRESHOLD;
            if (dollarsBetween50And100 > 0) {
                points += dollarsBetween50And100 * POINTS_RATE_BETWEEN_50_AND_100;
            }
        }
        return points;
    }

    /**
     * Calculates reward points for each customer per month and total,
     * by fetching all transactions from the database.
     *
     * @return A list of RewardSummary objects, one for each customer.
     */
    public List<RewardSummary> calculateRewardsForAllCustomers() {
        // Fetch all transactions from the database
        List<Transaction> transactions = transactionRepository.findAll();

        // Map to store RewardSummary for each customerId
        Map<String, RewardSummary> customerRewards = new HashMap<>();

        // DateTimeFormatter for extracting "YYYY-MM" from LocalDate
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Transaction transaction : transactions) {
            // Get or create RewardSummary for the customer
            // Use transaction.getCustomer().getCustomerId() to get the business customer ID
            RewardSummary summary = customerRewards.computeIfAbsent(
                    transaction.getCustomer().getCustomerId(),
                    RewardSummary::new
            );

            // Calculate points for the current transaction
            int points = calculatePoints(transaction.getAmount());

            // Get the month key (e.g., "2025-01")
            String monthKey = transaction.getTransactionDate().format(monthFormatter);

            // Add points to the summary
            summary.addPoints(monthKey, points);
        }

        // Convert the map values to a list and return
        return customerRewards.values().stream().collect(Collectors.toList());
    }

    /**
     * Calculates reward points for a specific customer for a given period.
     *
     * @param customerId The business ID of the customer.
     * @param startDate The start date of the period (inclusive).
     * @param endDate The end date of the period (inclusive).
     * @return A RewardSummary object for the specified customer.
     * @throws CustomerNotFoundException if no transactions are found for the customer in the specified period.
     */
    public RewardSummary calculateRewardsForCustomerInPeriod(String customerId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findByCustomer_CustomerIdAndTransactionDateBetween(customerId, startDate, endDate);

        if (transactions.isEmpty()) {
            // Throw custom exception if no transactions found for the customer in the period
            String period = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            throw new CustomerNotFoundException(customerId, period);
        }

        RewardSummary summary = new RewardSummary(customerId);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Transaction transaction : transactions) {
            int points = calculatePoints(transaction.getAmount());
            String monthKey = transaction.getTransactionDate().format(monthFormatter);
            summary.addPoints(monthKey, points);
        }
        return summary;
    }
}
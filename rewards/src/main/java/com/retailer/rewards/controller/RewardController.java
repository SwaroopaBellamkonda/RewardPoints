package com.retailer.rewards.controller;

import com.retailer.rewards.exception.CustomerAlreadyExistsException; // Import new exceptions
import com.retailer.rewards.exception.CustomerNotFoundException;    // Import new exceptions
import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import com.retailer.rewards.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing transactions and calculating customer reward points.
 */

@RestController
@RequestMapping
public class RewardController {

    private final RewardService rewardService;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public RewardController(RewardService rewardService, CustomerRepository customerRepository, TransactionRepository transactionRepository) {
        this.rewardService = rewardService;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Endpoint to save a new customer.
     *
     * @param customer The Customer object to save.
     * @return The saved Customer object.
     * @throws CustomerAlreadyExistsException if a customer with the given ID already exists.
     */
    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        // Check if customerId already exists to avoid duplicates
        Optional<Customer> existingCustomer = customerRepository.findByCustomerId(customer.getCustomerId());
        if (existingCustomer.isPresent()) {
            throw new CustomerAlreadyExistsException(customer.getCustomerId());
        }
        Customer savedCustomer = customerRepository.save(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
    }

    /**
     * Endpoint to save a new transaction.
     * Assumes the customer already exists in the database.
     *
     * @param transactionRequest A map containing "customerId", "amount", and "transactionDate".
     * @return The saved Transaction object.
     * @throws CustomerNotFoundException if the customer associated with the transaction is not found.
     */
    @PostMapping("/transactions")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Map<String, Object> transactionRequest) {
        String customerId = (String) transactionRequest.get("customerId");
        Double amount = ((Number) transactionRequest.get("amount")).doubleValue();
        LocalDate transactionDate = LocalDate.parse((String) transactionRequest.get("transactionDate"));

        Optional<Customer> customerOptional = customerRepository.findByCustomerId(customerId);
        if (customerOptional.isEmpty()) {
            throw new CustomerNotFoundException("Customer with ID '" + customerId + "' not found.");
        }

        Transaction transaction = new Transaction(customerOptional.get(), amount, transactionDate);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);
    }

    /**
     * Endpoint to calculate reward points for all customers based on all transactions in the database.
     *
     * @return A list of RewardSummary objects, detailing points per customer per month and total.
     */
    @GetMapping("/rewards/calculate/all")
    public ResponseEntity<List<RewardSummary>> calculateAllRewardPoints() {
        List<RewardSummary> rewardSummaries = rewardService.calculateRewardsForAllCustomers();
        if (rewardSummaries.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content if no rewards calculated
        }
        return ResponseEntity.ok(rewardSummaries);
    }

    /**
     * Endpoint to calculate reward points for a specific customer within a given date range.
     * Example: /rewards/calculate/CUST001?startDate=2025-01-01&endDate=2025-03-31
     *
     * @param customerId The business ID of the customer.
     * @param startDate The start date for the period (YYYY-MM-DD).
     * @param endDate The end date for the period (YYYY-MM-DD).
     * @return A RewardSummary object for the specified customer.
     * @throws CustomerNotFoundException if the customer is not found or has no transactions in the specified period.
     */
    @GetMapping("/rewards/calculate/{customerId}")
    public ResponseEntity<RewardSummary> calculateCustomerRewardPoints(
            @PathVariable String customerId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        // The service layer will now throw CustomerNotFoundException if no transactions are found
        // for the customer in the given period, which the GlobalExceptionHandler will catch.
        RewardSummary summary = rewardService.calculateRewardsForCustomerInPeriod(customerId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
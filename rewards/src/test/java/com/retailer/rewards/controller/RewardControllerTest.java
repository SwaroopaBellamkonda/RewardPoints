package com.retailer.rewards.controller;

import com.retailer.rewards.exception.CustomerAlreadyExistsException;
import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import com.retailer.rewards.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RewardController class.
 * Uses Mockito to mock all dependencies (RewardService, CustomerRepository, TransactionRepository).
 */
public class RewardControllerTest {

    @Mock
    private RewardService rewardService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RewardController rewardController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks before each test
    }

    /**
     * Test POST /customers - successful creation.
     */
    @Test
    void testCreateCustomer_success() {
        Customer newCustomer = new Customer("CUST001", "Alice");
        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.empty()); // Customer does not exist
        when(customerRepository.save(any(Customer.class))).thenReturn(newCustomer); // Save returns the customer

        ResponseEntity<Customer> response = rewardController.createCustomer(newCustomer);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CUST001", response.getBody().getCustomerId());
        verify(customerRepository, times(1)).findByCustomerId("CUST001");
        verify(customerRepository, times(1)).save(newCustomer);
    }

    /**
     * Test POST /customers - conflict when customerId already exists.
     * Expects CustomerAlreadyExistsException to be thrown.
     */
    @Test
    void testCreateCustomer_conflict() {
        Customer existingCustomer = new Customer("CUST001", "Alice");
        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(existingCustomer)); // Customer exists

        // Assert that the specific exception is thrown
        CustomerAlreadyExistsException thrown = assertThrows(CustomerAlreadyExistsException.class, () -> {
            rewardController.createCustomer(existingCustomer);
        });

        assertEquals("Customer with ID 'CUST001' already exists.", thrown.getMessage());
        verify(customerRepository, times(1)).findByCustomerId("CUST001");
        verify(customerRepository, never()).save(any(Customer.class)); // Save should not be called
    }

    /**
     * Test POST /transactions - successful creation.
     */
    @Test
    void testCreateTransaction_success() {
        Customer existingCustomer = new Customer("CUST001", "Alice");
        Map<String, Object> transactionRequest = new HashMap<>();
        transactionRequest.put("customerId", "CUST001");
        transactionRequest.put("amount", 120.00);
        transactionRequest.put("transactionDate", "2025-01-15");

        Transaction savedTransaction = new Transaction(existingCustomer, 120.00, LocalDate.of(2025, 1, 15));

        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(existingCustomer));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        ResponseEntity<Transaction> response = rewardController.createTransaction(transactionRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(120.00, response.getBody().getAmount());
        assertEquals(LocalDate.of(2025, 1, 15), response.getBody().getTransactionDate());
        assertEquals("CUST001", response.getBody().getCustomer().getCustomerId());
        verify(customerRepository, times(1)).findByCustomerId("CUST001");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    /**
     * Test POST /transactions - customer not found.
     * Expects CustomerNotFoundException to be thrown.
     */
    @Test
    void testCreateTransaction_customerNotFound() {
        Map<String, Object> transactionRequest = new HashMap<>();
        transactionRequest.put("customerId", "NONEXISTENT");
        transactionRequest.put("amount", 100.00);
        transactionRequest.put("transactionDate", "2025-04-01");

        when(customerRepository.findByCustomerId("NONEXISTENT")).thenReturn(Optional.empty());

        // Assert that the specific exception is thrown
        CustomerNotFoundException thrown = assertThrows(CustomerNotFoundException.class, () -> {
            rewardController.createTransaction(transactionRequest);
        });

        assertEquals("Customer with ID 'NONEXISTENT' not found.", thrown.getMessage());
        verify(customerRepository, times(1)).findByCustomerId("NONEXISTENT");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    /**
     * Test GET /rewards/calculate/all - successful calculation with data.
     */
    @Test
    void testCalculateAllRewardPoints_success() {
        RewardSummary summary1 = new RewardSummary("CUST001");
        summary1.addPoints("2025-01", 115);
        summary1.addPoints("2025-03", 150);
        summary1.setTotalRewardPoints(265);

        RewardSummary summary2 = new RewardSummary("CUST002");
        summary2.addPoints("2025-02", 250);
        summary2.addPoints("2025-03", 70);
        summary2.setTotalRewardPoints(320);

        List<RewardSummary> mockSummaries = Arrays.asList(summary1, summary2);

        when(rewardService.calculateRewardsForAllCustomers()).thenReturn(mockSummaries);

        ResponseEntity<List<RewardSummary>> response = rewardController.calculateAllRewardPoints();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(265, response.getBody().get(0).getTotalRewardPoints()); // Assuming order, but better to check by customerId
        verify(rewardService, times(1)).calculateRewardsForAllCustomers();
    }

    /**
     * Test GET /rewards/calculate/{customerId} - successful calculation for specific customer.
     */
    @Test
    void testCalculateCustomerRewardPoints_success() {
        String customerId = "CUST001";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        RewardSummary mockSummary = new RewardSummary(customerId);
        mockSummary.addPoints("2025-01", 115);
        mockSummary.addPoints("2025-03", 150);
        mockSummary.setTotalRewardPoints(265);

        when(rewardService.calculateRewardsForCustomerInPeriod(eq(customerId), eq(startDate), eq(endDate)))
                .thenReturn(mockSummary);

        ResponseEntity<RewardSummary> response = rewardController.calculateCustomerRewardPoints(customerId, startDate, endDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(customerId, response.getBody().getCustomerId());
        assertEquals(265, response.getBody().getTotalRewardPoints());
        verify(rewardService, times(1)).calculateRewardsForCustomerInPeriod(customerId, startDate, endDate);
    }

    /**
     * Test GET /rewards/calculate/{customerId} - customer not found by service (no transactions).
     * Expects CustomerNotFoundException to be thrown.
     */
    @Test
    void testCalculateCustomerRewardPoints_notFound() {
        String customerId = "NONEXISTENT";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        String expectedErrorMessage = String.format("Customer with ID '%s' not found or has no transactions for the period: %s to %s",
                customerId, startDate, endDate);

        // Configure the mocked service to throw the exception
        when(rewardService.calculateRewardsForCustomerInPeriod(eq(customerId), eq(startDate), eq(endDate)))
                .thenThrow(new CustomerNotFoundException(customerId, startDate.toString() + " to " + endDate.toString()));

        // Assert that the specific exception is thrown by the controller method
        CustomerNotFoundException thrown = assertThrows(CustomerNotFoundException.class, () -> {
            rewardController.calculateCustomerRewardPoints(customerId, startDate, endDate);
        });

        assertEquals(expectedErrorMessage, thrown.getMessage());
        verify(rewardService, times(1)).calculateRewardsForCustomerInPeriod(customerId, startDate, endDate);
    }
}
package com.retailer.rewards.service;

import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException; // Import the exception
import com.retailer.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the RewardService class.
 * Uses Mockito to mock the TransactionRepository.
 */

public class RewardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks before each test
    }

    /**
     * Test cases for calculatePoints method.
     */
    @Test
    void testCalculatePoints() {
        // Amount <= $50: 0 points
        assertEquals(0, rewardService.calculatePoints(49.99), "Amount $49.99 should yield 0 points");
        assertEquals(0, rewardService.calculatePoints(50.00), "Amount $50.00 should yield 0 points");

        // Amount between $50 and $100: 1 point per dollar over $50
        assertEquals(1, rewardService.calculatePoints(51.00), "Amount $51.00 should yield 1 point");
        assertEquals(49, rewardService.calculatePoints(99.00), "Amount $99.00 should yield 49 points");
        assertEquals(50, rewardService.calculatePoints(100.00), "Amount $100.00 should yield 50 points"); // 1 * (100-50) = 50

        // Amount over $100: 2 points per dollar over $100 + 1 point per dollar between $50 and $100
        assertEquals(52, rewardService.calculatePoints(101.00), "Amount $101.00 should yield 52 points"); // (1*50) + (2*1) = 52
        assertEquals(90, rewardService.calculatePoints(120.00), "Amount $120.00 should yield 90 points"); // (1*50) + (2*20) = 50 + 40 = 90
        assertEquals(150, rewardService.calculatePoints(150.00), "Amount $150.00 should yield 150 points"); // (1*50) + (2*50) = 50 + 100 = 150
        assertEquals(250, rewardService.calculatePoints(200.00), "Amount $200.00 should yield 250 points"); // (1*50) + (2*100) = 50 + 200 = 250
    }

    /**
     * Test calculateRewardsForAllCustomers with multiple customers and transactions.
     */
    @Test
    void testCalculateRewardsForAllCustomers_multipleCustomersMultipleTransactions() {
        // Setup mock data
        Customer cust1 = new Customer("CUST001", "Alice");
        Customer cust2 = new Customer("CUST002", "Bob");
        Customer cust3 = new Customer("CUST003", "Charlie");

        List<Transaction> transactions = Arrays.asList(
                new Transaction(cust1, 120.00, LocalDate.of(2025, 1, 15)), // CUST001: 90 points (50 + 2*20)
                new Transaction(cust1, 75.00, LocalDate.of(2025, 1, 20)),  // CUST001: 25 points (1*25)
                new Transaction(cust2, 200.00, LocalDate.of(2025, 2, 1)),  // CUST002: 250 points (50 + 2*100)
                new Transaction(cust1, 40.00, LocalDate.of(2025, 2, 10)),  // CUST001: 0 points
                new Transaction(cust2, 110.00, LocalDate.of(2025, 3, 5)),  // CUST002: 70 points (50 + 2*10)
                new Transaction(cust3, 100.00, LocalDate.of(2025, 3, 10)), // CUST003: 50 points (1*50)
                new Transaction(cust1, 150.00, LocalDate.of(2025, 3, 25))  // CUST001: 150 points (50 + 2*50)
        );
        when(transactionRepository.findAll()).thenReturn(transactions);

        // Call the service method
        List<RewardSummary> summaries = rewardService.calculateRewardsForAllCustomers();

        // Assertions
        assertNotNull(summaries);
        assertEquals(3, summaries.size(), "Should return summaries for 3 customers");

        // Verify CUST001
        RewardSummary cust1Summary = summaries.stream()
                .filter(s -> s.getCustomerId().equals("CUST001"))
                .findFirst()
                .orElse(null);
        assertNotNull(cust1Summary);
        assertEquals(90 + 25 + 150, cust1Summary.getTotalRewardPoints(), "CUST001 total points mismatch");
        // Corrected assertion: CUST001 has transactions in Jan, Feb (0 points), and Mar
        assertEquals(3, cust1Summary.getMonthlyRewardPoints().size(), "CUST001 should have points for 3 months");
        assertEquals(90 + 25, cust1Summary.getMonthlyRewardPoints().get("2025-01"), "CUST001 Jan points mismatch");
        assertEquals(0, cust1Summary.getMonthlyRewardPoints().get("2025-02"), "CUST001 Feb points mismatch (should be 0)"); // Explicitly check 0-point month
        assertEquals(150, cust1Summary.getMonthlyRewardPoints().get("2025-03"), "CUST001 Mar points mismatch");


        // Verify CUST002
        RewardSummary cust2Summary = summaries.stream()
                .filter(s -> s.getCustomerId().equals("CUST002"))
                .findFirst()
                .orElse(null);
        assertNotNull(cust2Summary);
        assertEquals(250 + 70, cust2Summary.getTotalRewardPoints(), "CUST002 total points mismatch");
        assertEquals(2, cust2Summary.getMonthlyRewardPoints().size(), "CUST002 should have points for 2 months");
        assertEquals(250, cust2Summary.getMonthlyRewardPoints().get("2025-02"), "CUST002 Feb points mismatch");
        assertEquals(70, cust2Summary.getMonthlyRewardPoints().get("2025-03"), "CUST002 Mar points mismatch");

        // Verify CUST003
        RewardSummary cust3Summary = summaries.stream()
                .filter(s -> s.getCustomerId().equals("CUST003"))
                .findFirst()
                .orElse(null);
        assertNotNull(cust3Summary);
        assertEquals(50, cust3Summary.getTotalRewardPoints(), "CUST003 total points mismatch");
        assertEquals(1, cust3Summary.getMonthlyRewardPoints().size(), "CUST003 should have points for 1 month");
        assertEquals(50, cust3Summary.getMonthlyRewardPoints().get("2025-03"), "CUST003 Mar points mismatch");
    }

    /**
     * Test calculateRewardsForAllCustomers when no transactions are present.
     */
    @Test
    void testCalculateRewardsForAllCustomers_noTransactions() {
        when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

        List<RewardSummary> summaries = rewardService.calculateRewardsForAllCustomers();

        assertNotNull(summaries);
        assertTrue(summaries.isEmpty(), "Should return an empty list when no transactions");
    }

    /**
     * Test calculateRewardsForCustomerInPeriod for a specific customer and period.
     */
    @Test
    void testCalculateRewardsForCustomerInPeriod_found() {
        Customer cust1 = new Customer("CUST001", "Alice");
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        List<Transaction> cust1Transactions = Arrays.asList(
                new Transaction(cust1, 120.00, LocalDate.of(2025, 1, 15)), // 90 points
                new Transaction(cust1, 75.00, LocalDate.of(2025, 1, 20)),  // 25 points
                new Transaction(cust1, 40.00, LocalDate.of(2025, 2, 10)),  // 0 points
                new Transaction(cust1, 150.00, LocalDate.of(2025, 3, 25))  // 150 points
        );
        when(transactionRepository.findByCustomer_CustomerIdAndTransactionDateBetween("CUST001", startDate, endDate))
                .thenReturn(cust1Transactions);

        RewardSummary summary = rewardService.calculateRewardsForCustomerInPeriod("CUST001", startDate, endDate);

        assertNotNull(summary);
        assertEquals("CUST001", summary.getCustomerId());
        assertEquals(90 + 25 + 150, summary.getTotalRewardPoints());
        assertEquals(3, summary.getMonthlyRewardPoints().size()); // Corrected assertion
        assertEquals(90 + 25, summary.getMonthlyRewardPoints().get("2025-01"));
        assertEquals(0, summary.getMonthlyRewardPoints().get("2025-02")); // Explicitly check 0-point month
        assertEquals(150, summary.getMonthlyRewardPoints().get("2025-03"));
    }

    /**
     * Negative test: calculateRewardsForCustomerInPeriod when customer has no transactions in the period.
     * This test now expects a CustomerNotFoundException.
     */
    @Test
    void testCalculateRewardsForCustomerInPeriod_noTransactionsInPeriod() {
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 30);
        String customerId = "CUST001";
        String expectedErrorMessage = String.format("Customer with ID '%s' not found or has no transactions for the period: %s to %s",
                customerId, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));


        when(transactionRepository.findByCustomer_CustomerIdAndTransactionDateBetween(customerId, startDate, endDate))
                .thenReturn(Collections.emptyList());

        // Assert that CustomerNotFoundException is thrown
        CustomerNotFoundException thrown = assertThrows(CustomerNotFoundException.class, () -> {
            rewardService.calculateRewardsForCustomerInPeriod(customerId, startDate, endDate);
        });

        assertEquals(expectedErrorMessage, thrown.getMessage());
    }

    /**
     * Negative test: calculateRewardsForCustomerInPeriod for a non-existent customer (repository returns empty).
     * This test now expects a CustomerNotFoundException.
     */
    @Test
    void testCalculateRewardsForCustomerInPeriod_nonExistentCustomer() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        String customerId = "NONEXISTENT";
        String expectedErrorMessage = String.format("Customer with ID '%s' not found or has no transactions for the period: %s to %s",
                customerId, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(transactionRepository.findByCustomer_CustomerIdAndTransactionDateBetween(customerId, startDate, endDate))
                .thenReturn(Collections.emptyList());

        // Assert that CustomerNotFoundException is thrown
        CustomerNotFoundException thrown = assertThrows(CustomerNotFoundException.class, () -> {
            rewardService.calculateRewardsForCustomerInPeriod(customerId, startDate, endDate);
        });

        assertEquals(expectedErrorMessage, thrown.getMessage());
    }
}
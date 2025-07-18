package com.retailer.rewards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the RewardController.
 * Uses MockMvc to simulate HTTP requests.
 */

@SpringBootTest
@AutoConfigureMockMvc
public class RewardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Used for converting objects to JSON and vice-versa

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Clear the database and pre-populate with test data before each test.
     */
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll(); // Delete transactions first due to foreign key constraint
        customerRepository.deleteAll();

        // Pre-populate customers
        customerRepository.save(new Customer("CUST001", "Alice"));
        customerRepository.save(new Customer("CUST002", "Bob"));
        customerRepository.save(new Customer("CUST003", "Charlie"));

        // Pre-populate transactions for CUST001
        Customer cust1 = customerRepository.findByCustomerId("CUST001").orElseThrow();
        transactionRepository.save(new Transaction(cust1, 120.00, LocalDate.of(2025, 1, 15))); // 90 points
        transactionRepository.save(new Transaction(cust1, 75.00, LocalDate.of(2025, 1, 20)));  // 25 points
        transactionRepository.save(new Transaction(cust1, 40.00, LocalDate.of(2025, 2, 10)));  // 0 points
        transactionRepository.save(new Transaction(cust1, 150.00, LocalDate.of(2025, 3, 25))); // 150 points

        // Pre-populate transactions for CUST002
        Customer cust2 = customerRepository.findByCustomerId("CUST002").orElseThrow();
        transactionRepository.save(new Transaction(cust2, 200.00, LocalDate.of(2025, 2, 1)));  // 250 points
        transactionRepository.save(new Transaction(cust2, 110.00, LocalDate.of(2025, 3, 5)));  // 70 points

        // Pre-populate transactions for CUST003
        Customer cust3 = customerRepository.findByCustomerId("CUST003").orElseThrow();
        transactionRepository.save(new Transaction(cust3, 100.00, LocalDate.of(2025, 3, 10))); // 50 points
    }

    /**
     * Test POST /customers - successful creation.
     */
    @Test
    void testCreateCustomer_success() throws Exception {
        Customer newCustomer = new Customer("CUST004", "David");

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST004"))
                .andExpect(jsonPath("$.name").value("David"));
    }

    /**
     * Test POST /customers - conflict when customerId already exists.
     */
    @Test
    void testCreateCustomer_conflict() throws Exception {
        Customer existingCustomer = new Customer("CUST001", "Alice"); // CUST001 already exists

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingCustomer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Customer with ID 'CUST001' already exists."));
    }

    /**
     * Test POST /transactions - successful creation.
     */
    @Test
    void testCreateTransaction_success() throws Exception {
        Map<String, Object> transactionRequest = new HashMap<>();
        transactionRequest.put("customerId", "CUST001");
        transactionRequest.put("amount", 60.00);
        transactionRequest.put("transactionDate", "2025-04-01");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer.customerId").value("CUST001"))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.transactionDate").value("2025-04-01"));
    }

    /**
     * Test POST /transactions - customer not found.
     */
    @Test
    void testCreateTransaction_customerNotFound() throws Exception {
        Map<String, Object> transactionRequest = new HashMap<>();
        transactionRequest.put("customerId", "NONEXISTENT");
        transactionRequest.put("amount", 100.00);
        transactionRequest.put("transactionDate", "2025-04-01");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer with ID 'NONEXISTENT' not found."));
    }

    /**
     * Test GET /rewards/calculate/all - successful calculation with data.
     */
    @Test
    void testCalculateAllRewardPoints_success() throws Exception {
        mockMvc.perform(get("/rewards/calculate/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // Expect 3 customers
                .andExpect(jsonPath("$[?(@.customerId == 'CUST001')].totalRewardPoints").value(hasItem(265))) // Corrected JSON path
                .andExpect(jsonPath("$[?(@.customerId == 'CUST002')].totalRewardPoints").value(hasItem(320))) // Corrected JSON path
                .andExpect(jsonPath("$[?(@.customerId == 'CUST003')].totalRewardPoints").value(hasItem(50))); // Corrected JSON path
    }

    /**
     * Test GET /rewards/calculate/all - no content when no transactions.
     */
    @Test
    void testCalculateAllRewardPoints_noContent() throws Exception {
        transactionRepository.deleteAll(); // Clear all transactions
        customerRepository.deleteAll(); // Clear all customers

        mockMvc.perform(get("/rewards/calculate/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // 204 No Content
    }

    /**
     * Test GET /rewards/calculate/{customerId} - successful calculation for specific customer.
     */
    @Test
    void testCalculateCustomerRewardPoints_success() throws Exception {
        mockMvc.perform(get("/rewards/calculate/CUST001")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-03-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.totalRewardPoints").value(265)) // Corrected JSON path
                .andExpect(jsonPath("$.monthlyRewardPoints.2025-01").value(115)) // Corrected JSON path
                .andExpect(jsonPath("$.monthlyRewardPoints.2025-03").value(150)) // Corrected JSON path
                .andExpect(jsonPath("$.monthlyRewardPoints.2025-02").value(0)); // Corrected: Expect 0 points for Feb, not absence
    }

    /**
     * Test GET /rewards/calculate/{customerId} - customer not found or no transactions in period.
     */
    @Test
    void testCalculateCustomerRewardPoints_customerNotFoundOrNoTransactionsInPeriod() throws Exception {
        // CUST001 has no transactions in April 2025 in our setup, leading to CustomerNotFoundException
        String customerId = "CUST001";
        String startDate = "2025-04-01";
        String endDate = "2025-04-30";
        String expectedMessage = String.format("Customer with ID '%s' not found or has no transactions for the period: %s to %s",
                customerId, startDate, endDate);

        mockMvc.perform(get("/rewards/calculate/" + customerId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(expectedMessage));

        // Test with a completely non-existent customer ID
        customerId = "NONEXISTENT";
        expectedMessage = String.format("Customer with ID '%s' not found or has no transactions for the period: %s to %s",
                customerId, startDate, endDate);
        mockMvc.perform(get("/rewards/calculate/" + customerId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    /**
     * Test GET /rewards/calculate/{customerId} - invalid date format.
     */
    @Test
    void testCalculateCustomerRewardPoints_invalidDateFormat() throws Exception {
        mockMvc.perform(get("/rewards/calculate/CUST001")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2025-03-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request")) // Expect "Bad Request" as per GlobalExceptionHandler
                .andExpect(jsonPath("$.message").value(containsString("Parameter 'startDate' has invalid value 'invalid-date'. Expected type: LocalDate"))); // Updated message check
    }
}
package com.retailer.rewards.repository;

import com.retailer.rewards.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Customer entities.
 * Provides CRUD operations and custom queries for Transaction.
 */

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String CustomerId);
}
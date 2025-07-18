package com.retailer.rewards.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Represents a single customer transaction. This entity is now mapped to a database table. */

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Primary key for the transaction

    @ManyToOne // Many transactions can belong to one customer
    @JoinColumn(name = "customer_db_id", nullable = false) // Foreign key column
    private Customer customer; // Reference to the Customer entity

    private double amount;
    private LocalDate transactionDate;

    // Default constructor for JPA
    public Transaction(){

    }

    public Transaction(Customer customer, double amount, LocalDate transactionDate) {
        this.customer = customer;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", customer=" + customer +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                '}';
    }
}

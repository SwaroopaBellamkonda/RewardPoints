# RewardPoints
A retailer offers a rewards program to its customers, awarding points based on each recorded purchase.

## Retailer Reward Points Calculator
This Spring Boot application implements a reward points program for a retailer. It calculates reward points for customers based on their transactions, providing monthly and total reward summaries. The application uses an MYSQL database for data persistence and exposes RESTful API endpoints for managing customers, transactions, and calculating rewards.

## Table of Contents
1. Project Overview
2. Features
3. Technologies Used
4. Project Structure
5. Reward Calculation Logic
6. Database
7. Setup and Run
8. API Endpoints
9. Testing

## 1. Project Overview
The core purpose of this application is to automate the calculation of reward points for a retailer's customers. Customers earn points based on their purchase amounts:

- 2 points for every dollar spent over $100 in a single transaction.
- 1 point for every dollar spent between $50 and $100 in a single transaction.

The application provides functionality to:
- Store customer information.
- Record individual transactions.
- Calculate reward points for all customers per month and in total.
- Calculate reward points for a specific customer within a defined period.

## 2. Features
- RESTful API: Exposes endpoints for customer management, transaction recording, and reward calculation.
- Reward Point Calculation: Implements the specified business logic for calculating points per transaction.
- Monthly and Total Summaries: Aggregates points per customer on a monthly basis and provides a grand total.
- Data Persistence: Uses Spring Data JPA with an MYSQL database.
- Unit and Integration Tests: Comprehensive test suite to ensure correctness and reliability.

## 3. Technologies Used
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- MYSQL Database
- Lombok
- JUnit 5
- Mockito
- Jackson

## 4. Project Structure
```
rewards/
├── src/main/java/com/rewards/
│   ├── RewardsApplication.java
│   ├── controller/
│   │   └── RewardController.java
│   ├── model/
│   │   ├── Customer.java
│   │   ├── RewardSummary.java
│   │   └── Transaction.java
│   ├── repository/
│   │   ├── CustomerRepository.java
│   │   └── TransactionRepository.java
│   └── service/
│       └── RewardService.java
├── src/main/resources/
│   └── application.properties
└── src/test/java/com/retailer/rewards/
    ├── controller/
    │   └── RewardControllerTest.java
    └── service/
        └── RewardServiceTest.java
└── pom.xml
```

## 5. Reward Calculation Logic
The RewardService.calculatePoints(double amount) method implements the core logic:
- If amount > 100: Points = (amount - 100) * 2 + (100 - 50) * 1
- If 50 < amount <= 100: Points = (amount - 50) * 1
- If amount <= 50: Points = 0

The calculateRewardsForAllCustomers() and calculateRewardsForCustomerInPeriod() methods aggregate transaction-level points into monthly and total summaries for customers.

## 6. Database
The application uses an MYSQL database.

MYSQL Console: http://localhost:8080/MYSQL-console  
JDBC URL: jdbc:MYSQL:mem:customer_rewards_db  
Username: root
Password: root

## 7. Setup and Run
**Prerequisites**
- Java 17 or higher
- Maven

**Steps**
```bash
git clone <repository-url>
cd retailer-rewards
mvn clean install
mvn spring-boot:run
```
App starts on http://localhost:8080

## 8. API Endpoints

### Customers
**POST /customers**
```json
{
  "customerId": "CUST001",
  "name": "Alice"
}
```

### Transactions
**POST /transactions**
```json
{
  "customerId": "CUST001",
  "amount": 120.00,
  "transactionDate": "2025-01-15"
}
```

### Rewards
**GET /rewards/calculate/all**  
Returns reward summary for all customers.

**GET /rewards/calculate/{customerId}?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD**  
Returns reward summary for a specific customer in a given date range.

## 9. Testing
Run all tests:
```bash
mvn test
```

Includes unit tests for RewardService and integration tests for RewardController.

package com.retailer.rewards.dto;

import java.util.HashMap;
import java.util.Map;

/** Represents the reward points summary for a customer. */
public class RewardSummary {

    private String customerId;
    private Map<String, Integer> monthlyRewardPoints; // Key: "YYYY-MM", Value: points for that month
    private int totalRewardPoints;

    public RewardSummary(String customerId) {
        this.customerId = customerId;
        this.monthlyRewardPoints = new HashMap<>();
        this.totalRewardPoints = 0;
    }

    // Default constructor for JSON serialization/deserialization
    public RewardSummary() {
        this.monthlyRewardPoints = new HashMap<>();
    }

    // Getters and Setters

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Map<String, Integer> getMonthlyRewardPoints() {
        return monthlyRewardPoints;
    }

    public void setMonthlyRewardPoints(Map<String, Integer> monthlyRewardPoints) {
        this.monthlyRewardPoints = monthlyRewardPoints;
    }

    public int getTotalRewardPoints() {
        return totalRewardPoints;
    }

    public void setTotalRewardPoints(int totalRewardPoints) {
        this.totalRewardPoints = totalRewardPoints;
    }

    /**
     * Adds points to a specific month and updates the total.
     * @param monthKey The month in "YYYY-MM" format.
     * @param points The points to add.
     */
    public void addPoints(String monthKey, int points){
        this.monthlyRewardPoints.merge(monthKey, points, Integer::sum);
        this.totalRewardPoints += points;
    }

    @Override
    public String toString() {
        return "RewardSummary{" +
                "customerId='" + customerId + '\'' +
                ", monthlyRewardPoints=" + monthlyRewardPoints +
                ", totalRewardPoints=" + totalRewardPoints +
                '}';
    }
}

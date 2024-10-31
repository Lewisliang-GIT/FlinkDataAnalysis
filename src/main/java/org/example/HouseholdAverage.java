package org.example;

import java.time.LocalDateTime;

// Define Average calculation structure
class HouseholdAverage {
    public int householdId;
    public double average;
    public LocalDateTime timestamp;

    public int getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(int householdId) {
        this.householdId = householdId;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public HouseholdAverage(int householdId, double average, LocalDateTime timestamp) {
        this.householdId = householdId;
        this.average = average;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HouseholdAverage{" +
                "householdId=" + householdId +
                ", average=" + average +
                ", timestamp=" + timestamp +
                '}';
    }
}

package org.example;

import java.time.LocalDateTime;

public class Households {
    private int id;

    private LocalDateTime timeStamp;

    private double readingNumber;

    public Households() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getReadingNumber() {
        return readingNumber;
    }

    public void setReadingNumber(double readingNumber) {
        this.readingNumber = readingNumber;
    }

    public Households(int id, LocalDateTime timeStamp, double readingNumber) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.readingNumber = readingNumber;
    }

    @Override
    public String toString() {
        return "Households{" +
                "readingNumber=" + readingNumber +
                ", timeStamp=" + timeStamp +
                ", id=" + id +
                '}';
    }
}

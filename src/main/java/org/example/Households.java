package org.example;

public class Households {
    private int id;

    private long timeStamp;

    private double readingNumber;

    public Households() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getReadingNumber() {
        return readingNumber;
    }

    public void setReadingNumber(double readingNumber) {
        this.readingNumber = readingNumber;
    }

    public Households(int id, long timeStamp, double readingNumber) {
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

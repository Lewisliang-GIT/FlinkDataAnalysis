package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Random;

public class DataGenerator {
    public static void main(String[] args) {
        int numHouseholds = 10;
        int numHours = 24;

        List<Households> data = new ArrayList<>();

        Random random = new Random();

        for (int i = 0; i < numHouseholds; i++) {
            long timestamp = System.currentTimeMillis();
            for (int j = 0; j < numHours; j++) {
                Households household = new Households();
                household.setId(i);
                household.setTimeStamp(timestamp);
                household.setReadingNumber(random.nextDouble() * 100);
                data.add(household);
                timestamp += 3600000; // Increment timestamp by 1 hour
            }
        }

        // Sort the data by timestamp
        Collections.sort(data, (h1, h2) -> Long.compare(h1.getTimeStamp(), h2.getTimeStamp()));

        // Write the data to a text file
        try (FileWriter writer = new FileWriter("household_data.txt")) {
            for (Households household : data) {
                writer.write(household.getId() + "," + household.getTimeStamp() + "," + household.getReadingNumber() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

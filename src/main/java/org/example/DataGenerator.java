package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Random;

public class DataGenerator {
    public static void dataGenerator() {
        int numHouseholds = 10;
        int numHours = 24;

        List<Households> data = new ArrayList<>();

        Random random = new Random();

        for (int i = 0; i < numHouseholds; i++) {

            for (int j = 0; j < numHours; j++) {
                LocalDateTime timestamp = LocalDateTime.now().plusHours(j);
                Households household = new Households();
                household.setId(i);
                household.setTimeStamp(timestamp);
                household.setReadingNumber(random.nextDouble() * 100);
                data.add(household);
            }
        }

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

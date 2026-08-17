package tech.gusev.mcpulsor.server.service;

import java.util.Random;

public class HeartRateCalculator {
    public static final Random RANDOM = new Random();

    public static int getHeartRate(String name){
        return RANDOM.nextInt(100)+1;
    }
}

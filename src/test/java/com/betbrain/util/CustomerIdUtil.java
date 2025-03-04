package com.betbrain.util;

import java.util.Random;

public class CustomerIdUtil {

    public static int generate() {
        Random random = new Random();
        return random.nextInt(10000001);
    }
}



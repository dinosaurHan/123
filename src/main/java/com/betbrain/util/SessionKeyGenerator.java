package com.betbrain.util;

import java.util.Random;

/**
 * 会话生成工具类
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class SessionKeyGenerator {


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int KEY_LENGTH = 7;

    public static String generate() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}

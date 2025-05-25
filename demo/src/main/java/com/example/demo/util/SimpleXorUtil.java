package com.example.demo.util;

public class SimpleXorUtil {
    private static final byte KEY = 0x5A; // single-byte key

    public static String encrypt(String input) {
        if (input == null) return null;
        byte[] data = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; i++) {
            data[i] ^= KEY;
        }
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) return null;
        byte[] data = java.util.Base64.getDecoder().decode(cipherText);
        for (int i = 0; i < data.length; i++) {
            data[i] ^= KEY;
        }
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }
}

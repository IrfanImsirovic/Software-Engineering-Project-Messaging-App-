package com.example.demo.util;

public class SimpleXorUtil {
    private static final byte KEY = 0x5A; 

    public static String encrypt(String input) {
        if (input == null) return null;
        byte[] data = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; i++) {
            data[i] ^= KEY;
        }
        String result = java.util.Base64.getEncoder().encodeToString(data);
        return result;
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) return null;
        
        if (!isLikelyBase64(cipherText)) {
            return cipherText;
        }
        
        try {
            byte[] data = java.util.Base64.getDecoder().decode(cipherText);
            for (int i = 0; i < data.length; i++) {
                data[i] ^= KEY;
            }
            String result = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            return result;
        } catch (Exception e) {
            System.err.println("Failed to decrypt: '" + cipherText + "' - Error: " + e.getMessage());
            return cipherText; 
        }
    }
    
 
    public static boolean isLikelyBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        if (!str.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }
        
        if (str.length() % 4 != 0) {
            if (!str.endsWith("=") && !str.endsWith("==")) {
                return false;
            }
        }
        
        if (str.length() < 4) {
            return false;
        }
        
        if (str.startsWith("[") || str.startsWith("{")) {
            return false;
        }
        
        return true;
    }
}

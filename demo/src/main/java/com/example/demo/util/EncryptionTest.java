package com.example.demo.util;

public class EncryptionTest {
    public static void main(String[] args) {
        String original = "This is a test message";
        System.out.println("Original: " + original);
        
        String encrypted = SimpleXorUtil.encrypt(original);
        System.out.println("Encrypted: " + encrypted);
        
        String decrypted = SimpleXorUtil.decrypt(encrypted);
        System.out.println("Decrypted: " + decrypted);
    }
} 
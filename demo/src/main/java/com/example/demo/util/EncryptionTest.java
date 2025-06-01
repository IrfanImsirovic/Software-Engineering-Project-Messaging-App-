package com.example.demo.util;

public class EncryptionTest {
    public static void main(String[] args) {
        // Test encryption
        String original = "This is a test message";
        System.out.println("Original: " + original);
        
        String encrypted = SimpleXorUtil.encrypt(original);
        System.out.println("Encrypted: " + encrypted);
        
        String decrypted = SimpleXorUtil.decrypt(encrypted);
        System.out.println("Decrypted: " + decrypted);
        
        // Test if the encryption is actually changing the content
        if (original.equals(encrypted)) {
            System.out.println("ERROR: Encryption is not working! Original and encrypted are the same.");
        } else {
            System.out.println("SUCCESS: Encryption is working. Original and encrypted are different.");
        }
        
        // Test if decryption works
        if (original.equals(decrypted)) {
            System.out.println("SUCCESS: Decryption is working. Original and decrypted are the same.");
        } else {
            System.out.println("ERROR: Decryption is not working! Original and decrypted are different.");
        }
    }
} 
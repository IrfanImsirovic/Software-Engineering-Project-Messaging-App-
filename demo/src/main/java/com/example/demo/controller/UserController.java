package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam String username, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("profilePictureUrl", user.getProfilePictureUrl());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            Principal principal) {
        
        if (principal == null || !principal.getName().equals(username)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            // Check if the file is an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            // Create the upload directory if it doesn't exist
            File directory = new File(uploadDir + "/profiles");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save the file
            Path targetLocation = Paths.get(uploadDir + "/profiles").resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Update user profile picture URL in database
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String profilePictureUrl = "/uploads/profiles/" + newFilename;
            user.setProfilePictureUrl(profilePictureUrl);
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("url", profilePictureUrl);
            
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            return ResponseEntity.status(500)
                    .body("Could not upload profile picture: " + ex.getMessage());
        }
    }
} 
package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
@RequestMapping("/api/uploads")
public class FileUploadController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            // Check if the file is an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            // Create the upload directory if it doesn't exist
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate a unique filename to prevent collisions
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save the file
            Path targetLocation = Paths.get(uploadDir).resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return both API URL and direct file access URL
            String apiUrl = "/api/uploads/images/" + newFilename;
            String directUrl = "/uploads/" + newFilename;
            
            // Log the created file path for debugging
            System.out.println("📸 File saved at: " + targetLocation.toAbsolutePath());
            System.out.println("🔗 API URL: " + apiUrl);
            System.out.println("🔗 Direct URL: " + directUrl);

            Map<String, String> response = new HashMap<>();
            response.put("url", directUrl);  // Use the direct URL for better performance
            
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            return ResponseEntity.status(500)
                    .body("Could not upload the file: " + ex.getMessage());
        }
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            Path path = Paths.get(uploadDir).resolve(filename);
            byte[] imageBytes = Files.readAllBytes(path);
            
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "max-age=31536000, public") // Cache for 1 year
                    .header("Pragma", "cache")
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/test-access")
    public ResponseEntity<?> testAccess() {
        // Get the absolute path to the upload directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        File directory = uploadPath.toFile();
        
        Map<String, Object> response = new HashMap<>();
        response.put("directoryExists", directory.exists());
        response.put("directoryCanRead", directory.canRead());
        response.put("directoryCanWrite", directory.canWrite());
        response.put("directoryPath", uploadPath.toString());
        
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                response.put("fileCount", files.length);
                response.put("files", java.util.Arrays.stream(files)
                    .map(f -> Map.of(
                        "name", f.getName(),
                        "size", f.length(),
                        "readable", f.canRead(),
                        "lastModified", new java.util.Date(f.lastModified())
                    ))
                    .toArray());
            }
        }
        
        return ResponseEntity.ok(response);
    }
} 
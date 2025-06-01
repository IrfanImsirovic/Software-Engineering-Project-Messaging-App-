package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.FriendRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.FriendRequestRepository;
import com.example.demo.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.Optional;
import com.example.demo.dto.RegisterRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendRequestRepository friendRequestRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      FriendRequestRepository friendRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.friendRequestRepository = friendRequestRepository;
    }

    @PostConstruct
    private void initSecretKey() {
        this.secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public User registerUser(RegisterRequest registerRequest) {
        System.out.println("📝 Attempting to register user: " + registerRequest.getUsername());
    
        // Check if username exists
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            System.out.println("❌ Username already exists: " + registerRequest.getUsername());
            throw new RuntimeException("Username already exists");
        }
    
        // Check if email exists
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            System.out.println("❌ Email already exists: " + registerRequest.getEmail());
            throw new RuntimeException("Email already exists");
        }
    
        // Create a new User entity from the DTO
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        
        // Hash password
        System.out.println("🔑 Hashing password...");
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    
        // Save user
        User savedUser = userRepository.save(user);
        System.out.println("✅ User successfully registered: " + savedUser.getId());
    
        return savedUser;
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        System.out.println("🔍 Checking user: " + loginRequest.getUsername());
    
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
        if (userOptional.isEmpty()) {
            System.out.println("❌ User not found in database");
            throw new RuntimeException("User not found");
        }
    
        User user = userOptional.get();
        System.out.println("✅ User found: " + user.getUsername());
    
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            System.out.println("❌ Incorrect password");
            throw new RuntimeException("Invalid password");
        }
    
        System.out.println("✅ Password matches. Generating token...");
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(secretKey, SignatureAlgorithm.HS256) // ✅ not deprecated
                .compact();
    
        return new LoginResponse(token);
    }

    public void sendFriendRequest(String senderUsername, String receiverUsername) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderUsername));

        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverUsername));

        if (sender.equals(receiver)) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        if (friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, FriendRequest.Status.PENDING).isPresent()) {
            throw new RuntimeException("Friend request already sent");
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequest.Status.PENDING);

        friendRequestRepository.save(request);
    }

    public void acceptFriendRequest(String receiverUsername, String senderUsername) {
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverUsername));

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderUsername));

        FriendRequest request = friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, FriendRequest.Status.PENDING)
                .orElseThrow(() -> new RuntimeException("Pending friend request not found"));

        request.setStatus(FriendRequest.Status.ACCEPTED);
        friendRequestRepository.save(request);

        receiver.getFriends().add(sender);
        sender.getFriends().add(receiver);

        userRepository.save(receiver);
        userRepository.save(sender);
    }

    public void rejectFriendRequest(String receiverUsername, String senderUsername) {
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverUsername));

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderUsername));

        FriendRequest request = friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, FriendRequest.Status.PENDING)
                .orElseThrow(() -> new RuntimeException("Pending friend request not found"));

        request.setStatus(FriendRequest.Status.REJECTED);
        friendRequestRepository.save(request);
    }

    public void removeFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        User friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new RuntimeException("Friend not found: " + friendUsername));

        user.getFriends().remove(friend);
        friend.getFriends().remove(user);

        userRepository.save(user);
        userRepository.save(friend);
    }
    public List<String> findAllFriends(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFriends().stream()
                .map(User::getUsername)
                .toList();
    }
    
    public List<FriendRequest> getPendingFriendRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.Status.PENDING);
    }
}
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
    
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
    
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
    
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    
        User savedUser = userRepository.save(user);
    
        return savedUser;
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
    
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }
    
        User user = userOptional.get();
    
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
    
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) 
                .signWith(secretKey, SignatureAlgorithm.HS256) 
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
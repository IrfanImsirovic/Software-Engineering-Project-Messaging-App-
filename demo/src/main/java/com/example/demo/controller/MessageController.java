package com.example.demo.controller;

import com.example.demo.dto.MessageDTO;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.entity.ChatGroup;
import com.example.demo.entity.GroupMessage;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*; // ← added
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.Timestamp;

@Transactional
@RestController // ← change from @Controller to @RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;


    public MessageController(SimpMessagingTemplate messagingTemplate,
                             MessageRepository messageRepository,
                             UserRepository userRepository,
                             JdbcTemplate jdbcTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @MessageMapping("/chat")
    public void sendMessage(@Payload MessageDTO messageDTO, Principal principal) {
        if (principal == null) {
            System.out.println("❌ No user principal in WebSocket session");
            return;
        }

        String senderUsername = principal.getName();
        User sender = userRepository.findByUsername(senderUsername).orElse(null);
        User receiver = userRepository.findByUsername(messageDTO.getReceiver()).orElse(null);

        if (sender == null || receiver == null) return;

        boolean areFriends = sender.getFriends().stream()
                .anyMatch(friend -> friend.getUsername().equals(receiver.getUsername()));

        if (!areFriends) return;

        Message msg = new Message();
        msg.setSender(senderUsername);
        msg.setReceiver(receiver.getUsername());
        msg.setContent(messageDTO.getContent());
        msg.setTimestamp(LocalDateTime.now());

        messageRepository.save(msg);
        messagingTemplate.convertAndSend("/topic/messages/" + receiver.getUsername(), msg);
        messagingTemplate.convertAndSend("/topic/messages/" + senderUsername, msg);

    }

    // ✅ REST endpoint for chat history
    @GetMapping("/history")
    public List<Message> getChatHistory(@RequestParam String friendUsername, Principal principal) {
        String currentUsername = principal.getName();
        return messageRepository.findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
                currentUsername, friendUsername,
                currentUsername, friendUsername
        );
    }

    // REST endpoint for recent chats
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentChats(Principal principal) {
        try {
            String currentUsername = principal.getName();
            List<Object> combinedResults = new ArrayList<>();
            
            // 1. Get direct messages
            List<Message> directMessages = messageRepository.findBySenderOrReceiverOrderByTimestampDesc(
                    currentUsername, currentUsername);
            
            // Process the messages to get only the most recent one for each conversation
            Map<String, Message> latestMessagesByContact = new HashMap<>();
            
            for (Message message : directMessages) {
                String chatPartner;
                if (message.getSender().equals(currentUsername)) {
                    chatPartner = message.getReceiver();
                } else {
                    chatPartner = message.getSender();
                }
                
                // Only keep the latest message for each contact
                if (!latestMessagesByContact.containsKey(chatPartner) || 
                    message.getTimestamp().isAfter(latestMessagesByContact.get(chatPartner).getTimestamp())) {
                    latestMessagesByContact.put(chatPartner, message);
                }
            }
            
            // Add direct messages to combined results
            combinedResults.addAll(latestMessagesByContact.values());
            
            // 2. Get all groups the user is a member of - simplified approach
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                    
            List<ChatGroup> userGroups = jdbcTemplate.query(
                "SELECT g.* FROM chat_groups g " +
                "JOIN group_members gm ON g.id = gm.group_id " +
                "JOIN users u ON gm.user_id = u.id " +
                "WHERE u.username = ?",
                (rs, rowNum) -> {
                    ChatGroup group = new ChatGroup();
                    group.setId(rs.getLong("id"));
                    group.setName(rs.getString("name"));
                    return group;
                },
                currentUsername
            );
            
            // 3. For each group, find the latest message or create a placeholder
            for (ChatGroup group : userGroups) {
                Map<String, Object> groupEntry = new HashMap<>();
                groupEntry.put("id", group.getId());
                groupEntry.put("name", group.getName());
                groupEntry.put("isGroup", true);
                
                // Try to find the latest message for this group
                List<GroupMessage> groupMessages = jdbcTemplate.query(
                    "SELECT * FROM group_messages " +
                    "WHERE group_id = ? " +
                    "ORDER BY timestamp DESC LIMIT 1",
                    (rs, rowNum) -> {
                        GroupMessage msg = new GroupMessage();
                        msg.setId(rs.getLong("id"));
                        msg.setSender(rs.getString("sender"));
                        msg.setContent(rs.getString("content"));
                        msg.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                        return msg;
                    },
                    group.getId()
                );
                
                if (!groupMessages.isEmpty()) {
                    // Found a message, use it
                    GroupMessage latestMessage = groupMessages.get(0);
                    groupEntry.put("sender", latestMessage.getSender());
                    groupEntry.put("content", latestMessage.getContent());
                    groupEntry.put("timestamp", latestMessage.getTimestamp());
                } else {
                    // No message found, use placeholder
                    groupEntry.put("sender", "SYSTEM");
                    groupEntry.put("content", "New group created");
                    groupEntry.put("timestamp", LocalDateTime.now());
                }
                
                combinedResults.add(groupEntry);
            }
            
            // 4. Sort all entries by timestamp (most recent first)
            combinedResults.sort((a, b) -> {
                LocalDateTime timeA = null;
                LocalDateTime timeB = null;
                
                if (a instanceof Message) {
                    timeA = ((Message) a).getTimestamp();
                } else {
                    Object timestampA = ((Map<String, Object>) a).get("timestamp");
                    if (timestampA instanceof LocalDateTime) {
                        timeA = (LocalDateTime) timestampA;
                    }
                }
                
                if (b instanceof Message) {
                    timeB = ((Message) b).getTimestamp();
                } else {
                    Object timestampB = ((Map<String, Object>) b).get("timestamp");
                    if (timestampB instanceof LocalDateTime) {
                        timeB = (LocalDateTime) timestampB;
                    }
                }
                
                // If either timestamp is null, handle accordingly
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1; // Null timestamps go last
                if (timeB == null) return -1;
                
                return timeB.compareTo(timeA);
            });
            
            return ResponseEntity.ok(combinedResults);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching recent chats: " + e.getMessage());
        }
    }
}

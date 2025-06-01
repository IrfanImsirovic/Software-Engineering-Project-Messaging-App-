package com.example.demo.controller;

import com.example.demo.dto.MessageDTO;
import com.example.demo.entity.ChatGroup;
import com.example.demo.entity.GroupMessage;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SimpleXorUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Transactional
@RestController
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
            return;
        }

        String senderUsername = principal.getName();
        
        User sender = userRepository.findByUsername(senderUsername).orElse(null);
        User receiver = userRepository.findByUsername(messageDTO.getReceiver()).orElse(null);

        if (sender == null || receiver == null) return;

        boolean areFriends = sender.getFriends().stream()
                .anyMatch(friend -> friend.getUsername().equals(receiver.getUsername()));
        if (!areFriends) return;

        String originalContent = messageDTO.getContent();
        
        String encrypted = "";
        if (originalContent != null && !originalContent.isEmpty()) {
            encrypted = SimpleXorUtil.encrypt(originalContent);
        }

        Message msg = new Message();
        msg.setSender(senderUsername);
        msg.setReceiver(receiver.getUsername());
        msg.setContent(encrypted);
        msg.setTimestamp(LocalDateTime.now());
        msg.setImageUrl(messageDTO.getImageUrl());

        Message savedMsg = messageRepository.save(msg);

        Message clientMsg = new Message();
        clientMsg.setSender(senderUsername);
        clientMsg.setReceiver(receiver.getUsername());
        clientMsg.setContent(originalContent);
        clientMsg.setTimestamp(savedMsg.getTimestamp());
        clientMsg.setImageUrl(messageDTO.getImageUrl());

        messagingTemplate.convertAndSend("/topic/messages/" + receiver.getUsername(), clientMsg);
        messagingTemplate.convertAndSend("/topic/messages/" + senderUsername, clientMsg);
    }

    @GetMapping("/history")
    public List<Map<String, Object>> getChatHistory(@RequestParam String friendUsername, Principal principal) {
        String currentUsername = principal.getName();
        List<Message> history = messageRepository
                .findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
                        currentUsername, friendUsername,
                        currentUsername, friendUsername
                );

y        List<Map<String, Object>> result = new ArrayList<>();
        for (Message entity : history) {
            Map<String, Object> messageDto = new HashMap<>();
            messageDto.put("id", entity.getId());
            messageDto.put("sender", entity.getSender());
            messageDto.put("receiver", entity.getReceiver());
            messageDto.put("timestamp", entity.getTimestamp());
            messageDto.put("imageUrl", entity.getImageUrl());
            
            try {
                if (entity.getContent() != null && !entity.getContent().isEmpty()) {
                    String decrypted = SimpleXorUtil.decrypt(entity.getContent());
                    messageDto.put("content", decrypted);
                } else {
                    messageDto.put("content", "");
                }
            } catch (Exception e) {
                System.err.println("Error decrypting message ID " + entity.getId() + ": " + e.getMessage());
                messageDto.put("content", entity.getContent());
            }
            
            result.add(messageDto);
        }
        return result;
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentChats(Principal principal) {
        try {
            String currentUsername = principal.getName();
            List<Object> combinedResults = new ArrayList<>();

            List<Message> directMessages = messageRepository.findBySenderOrReceiverOrderByTimestampDesc(
                    currentUsername, currentUsername);

            Map<String, Message> latestByContact = new HashMap<>();
            for (Message message : directMessages) {
                String chatPartner = message.getSender().equals(currentUsername)
                        ? message.getReceiver()
                        : message.getSender();

                if (!latestByContact.containsKey(chatPartner)
                        || message.getTimestamp().isAfter(latestByContact.get(chatPartner).getTimestamp())) {
                    latestByContact.put(chatPartner, message);
                }
            }

            for (Message entity : latestByContact.values()) {
                try {
                    Map<String, Object> messageDto = new HashMap<>();
                    messageDto.put("id", entity.getId());
                    messageDto.put("sender", entity.getSender());
                    messageDto.put("receiver", entity.getReceiver());
                    messageDto.put("timestamp", entity.getTimestamp());
                    messageDto.put("imageUrl", entity.getImageUrl());
                    
                    if (entity.getContent() != null && !entity.getContent().isEmpty()) {
                        String decrypted = SimpleXorUtil.decrypt(entity.getContent());
                        messageDto.put("content", decrypted);
                    } else {
                        messageDto.put("content", "");
                    }
                    
                    combinedResults.add(messageDto);
                } catch (Exception e) {
                    System.err.println("Decryption failed for message ID " + entity.getId() + ": " + e.getMessage());
                    
                    Map<String, Object> messageDto = new HashMap<>();
                    messageDto.put("id", entity.getId());
                    messageDto.put("sender", entity.getSender());
                    messageDto.put("receiver", entity.getReceiver());
                    messageDto.put("timestamp", entity.getTimestamp());
                    messageDto.put("imageUrl", entity.getImageUrl());
                    messageDto.put("content", "[Message could not be decrypted]");
                    
                    combinedResults.add(messageDto);
                }
            }

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

            for (ChatGroup group : userGroups) {
                Map<String, Object> groupEntry = new HashMap<>();
                groupEntry.put("id", group.getId());
                groupEntry.put("name", group.getName());
                groupEntry.put("isGroup", true);

                List<GroupMessage> groupMessages = jdbcTemplate.query(
                        "SELECT * FROM group_messages WHERE group_id = ? ORDER BY timestamp DESC LIMIT 1",
                        (rs, rowNum) -> {
                            GroupMessage gm = new GroupMessage();
                            gm.setId(rs.getLong("id"));
                            gm.setSender(rs.getString("sender"));
                            
                            String content = rs.getString("content");
                            System.out.println("📝 Raw group message content: '" + content + "'");
                            gm.setContent(content);
                            
                            java.sql.Timestamp timestamp = rs.getTimestamp("timestamp");
                            if (timestamp != null) {
                                gm.setTimestamp(timestamp.toLocalDateTime());
                            } else {
                                gm.setTimestamp(LocalDateTime.now());
                            }
                            
                            return gm;
                        },
                        group.getId()
                );

                if (!groupMessages.isEmpty()) {
                    GroupMessage latest = groupMessages.get(0);
                    String plain = "No message content";
                    try {
                        if (latest.getContent() != null && !latest.getContent().isEmpty()) {
                            plain = SimpleXorUtil.decrypt(latest.getContent());
                        }
                    } catch (Exception e) {
                        plain = "[Message could not be decrypted]";
                        System.err.println("Error decrypting group message: " + e.getMessage());
                    }
                    
                    groupEntry.put("sender", latest.getSender());
                    groupEntry.put("content", plain);
                    groupEntry.put("timestamp", latest.getTimestamp());
                } else {
                    groupEntry.put("sender", "SYSTEM");
                    groupEntry.put("content", "New group created");
                    groupEntry.put("timestamp", LocalDateTime.now());
                }

                combinedResults.add(groupEntry);
            }

            combinedResults.sort((a, b) -> {
                LocalDateTime tA = extractTimestamp(a);
                LocalDateTime tB = extractTimestamp(b);
                if (tA == null && tB == null) return 0;
                if (tA == null) return 1;
                if (tB == null) return -1;
                return tB.compareTo(tA);
            });

            System.out.println("Successfully processed recent chats");
            return ResponseEntity.ok(combinedResults);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching recent chats: " + e.getMessage());
        }
    }

    private LocalDateTime extractTimestamp(Object obj) {
        if (obj instanceof Message) {
            return ((Message) obj).getTimestamp();
        } else if (obj instanceof Map) {
            Object ts = ((Map<?, ?>) obj).get("timestamp");
            if (ts instanceof LocalDateTime) {
                return (LocalDateTime) ts;
            }
        }
        return null;
    }

    @GetMapping("/test-encryption")
    public ResponseEntity<?> testEncryption(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        
        String testContent = "This is a test encryption message";        
        String encrypted = SimpleXorUtil.encrypt(testContent);
        
        Message testMsg = new Message();
        testMsg.setSender(principal.getName());
        testMsg.setReceiver("test-receiver");
        testMsg.setContent(encrypted);
        testMsg.setTimestamp(LocalDateTime.now());
        
        Message savedMsg = messageRepository.save(testMsg);
        
        Message retrievedMsg = messageRepository.findById(savedMsg.getId()).orElse(null);
        if (retrievedMsg != null) {
            String decrypted = SimpleXorUtil.decrypt(retrievedMsg.getContent());
            
            Map<String, Object> result = new HashMap<>();
            result.put("original", testContent);
            result.put("encrypted", encrypted);
            result.put("storedInDb", retrievedMsg.getContent());
            result.put("decrypted", decrypted);
            result.put("messageId", retrievedMsg.getId());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body("Failed to retrieve message");
        }
    }
}

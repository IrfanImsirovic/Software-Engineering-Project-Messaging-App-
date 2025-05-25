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

        // Encrypt text content
        String encrypted = SimpleXorUtil.encrypt(messageDTO.getContent());

        Message msg = new Message();
        msg.setSender(senderUsername);
        msg.setReceiver(receiver.getUsername());
        msg.setContent(encrypted);
        msg.setTimestamp(LocalDateTime.now());
        msg.setImageUrl(messageDTO.getImageUrl());

        messageRepository.save(msg);

        // Decrypt before sending back to clients
        msg.setContent(SimpleXorUtil.decrypt(encrypted));
        messagingTemplate.convertAndSend("/topic/messages/" + receiver.getUsername(), msg);
        messagingTemplate.convertAndSend("/topic/messages/" + senderUsername, msg);
    }

    // ✅ REST endpoint for chat history
    @GetMapping("/history")
    public List<Message> getChatHistory(@RequestParam String friendUsername, Principal principal) {
        String currentUsername = principal.getName();
        List<Message> history = messageRepository
                .findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
                        currentUsername, friendUsername,
                        currentUsername, friendUsername
                );

        // Decrypt each message before returning
        history.forEach(m -> m.setContent(SimpleXorUtil.decrypt(m.getContent())));
        return history;
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

            // Process to keep only most recent per contact
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

            // Decrypt direct messages and add
            latestByContact.values().forEach(m -> m.setContent(SimpleXorUtil.decrypt(m.getContent())));
            combinedResults.addAll(latestByContact.values());

            // 2. Get all groups the user is a member of
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

            // 3. For each group, find latest message or placeholder
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
                            gm.setContent(rs.getString("content"));
                            gm.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                            return gm;
                        },
                        group.getId()
                );

                if (!groupMessages.isEmpty()) {
                    GroupMessage latest = groupMessages.get(0);
                    // Decrypt group message content
                    String plain = SimpleXorUtil.decrypt(latest.getContent());
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

            // 4. Sort all entries by timestamp (most recent first)
            combinedResults.sort((a, b) -> {
                LocalDateTime tA = extractTimestamp(a);
                LocalDateTime tB = extractTimestamp(b);
                if (tA == null && tB == null) return 0;
                if (tA == null) return 1;
                if (tB == null) return -1;
                return tB.compareTo(tA);
            });

            return ResponseEntity.ok(combinedResults);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching recent chats: " + e.getMessage());
        }
    }

    // Helper to pull timestamp from Message or Map entry
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
}

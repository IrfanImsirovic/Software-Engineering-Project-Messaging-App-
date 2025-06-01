package com.example.demo.controller;

import com.example.demo.dto.GroupMessageDTO;
import com.example.demo.entity.ChatGroup;
import com.example.demo.entity.GroupMessage;
import com.example.demo.repository.ChatGroupRepository;
import com.example.demo.repository.GroupMessageRepository;
import com.example.demo.util.SimpleXorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/group-messages")
public class GroupMessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatGroupRepository chatGroupRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @MessageMapping("/group")
    public void sendGroupMessage(@Payload GroupMessageDTO dto, Principal principal) {
        ChatGroup group = chatGroupRepository.findByIdWithMembers(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isMember = group.getMembers().stream()
                .anyMatch(u -> u.getUsername().equals(principal.getName()));
        if (!isMember) {
            return;
        }

        String originalContent = dto.getContent();
        
        String encrypted = "";
        if (originalContent != null && !originalContent.isEmpty()) {
            encrypted = SimpleXorUtil.encrypt(originalContent);
        }

        GroupMessage message = new GroupMessage();
        message.setSender(principal.getName());
        message.setContent(encrypted); 
        message.setTimestamp(LocalDateTime.now());
        message.setGroup(group);
        message.setImageUrl(dto.getImageUrl());

        GroupMessage savedMessage = groupMessageRepository.save(message);

        GroupMessage clientMsg = new GroupMessage();
        clientMsg.setId(savedMessage.getId());
        clientMsg.setSender(principal.getName());
        clientMsg.setContent(originalContent); 
        clientMsg.setTimestamp(savedMessage.getTimestamp());
        clientMsg.setGroup(group);
        clientMsg.setImageUrl(dto.getImageUrl());

        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), clientMsg);
    }

    @GetMapping("/history")
    public List<GroupMessage> getGroupChatHistory(@RequestParam Long groupId, Principal principal) {
        ChatGroup group = chatGroupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isMember = group.getMembers().stream()
                .anyMatch(u -> u.getUsername().equals(principal.getName()));
        if (!isMember) {
            throw new RuntimeException("You are not a member of this group");
        }

        List<GroupMessage> messages = groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
        
        for (GroupMessage gm : messages) {
            try {
                if (gm.getContent() != null && !gm.getContent().isEmpty()) {
                    String decrypted = SimpleXorUtil.decrypt(gm.getContent());
                    gm.setContent(decrypted);
                } else {
                    gm.setContent("");
                }
            } catch (Exception e) {
                System.err.println("Error decrypting group message ID " + gm.getId() + ": " + e.getMessage());
            }
        }
        
        return messages;
    }
}

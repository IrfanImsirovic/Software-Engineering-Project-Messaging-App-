package com.example.demo.controller;

import com.example.demo.dto.GroupMessageDTO;
import com.example.demo.entity.ChatGroup;
import com.example.demo.entity.GroupMessage;
import com.example.demo.repository.ChatGroupRepository;
import com.example.demo.repository.GroupMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*; 

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

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
            System.out.println("❌ Not a member of this group.");
            return;
        }

        GroupMessage message = new GroupMessage();
        message.setSender(principal.getName());
        message.setContent(dto.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setGroup(group);

        groupMessageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), message);
        System.out.println("✅ Group message sent: " + message.getContent());

    }

    // ✅ REST endpoint to load group message history
    @GetMapping("/history")
    public List<GroupMessage> getGroupChatHistory(@RequestParam Long groupId, Principal principal) {
        ChatGroup group = chatGroupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isMember = group.getMembers().stream()
                .anyMatch(u -> u.getUsername().equals(principal.getName()));

        if (!isMember) {
            throw new RuntimeException("You are not a member of this group");
        }

        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
    }
    
}

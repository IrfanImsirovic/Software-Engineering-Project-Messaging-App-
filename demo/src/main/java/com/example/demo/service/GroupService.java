package com.example.demo.service;

import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.entity.ChatGroup;
import com.example.demo.entity.GroupMessage;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatGroupRepository;
import com.example.demo.repository.GroupMessageRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private ChatGroupRepository chatGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ChatGroup createGroup(String creatorUsername, CreateGroupRequest request) {
        ChatGroup group = new ChatGroup();
        group.setName(request.getName());

        Set<User> members = new HashSet<>();
        request.getMemberUsernames().forEach(username -> {
            userRepository.findByUsername(username).ifPresent(members::add);
        });

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        members.add(creator);
        group.setMembers(members);
        group.setOwner(creator);

        ChatGroup savedGroup = chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent(creator.getUsername() + " has created the group.");
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(savedGroup);

        groupMessageRepository.save(systemMessage);

        messagingTemplate.convertAndSend("/topic/group/" + savedGroup.getId(), systemMessage);

        return savedGroup;
    }

    public void addMemberToGroup(String requesterUsername, Long groupId, String usernameToAdd) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        if (!group.getOwner().equals(requester)) {
            throw new RuntimeException("Only the group owner can add members.");
        }

        User newUser = userRepository.findByUsername(usernameToAdd)
                .orElseThrow(() -> new RuntimeException("User to add not found"));

        if (group.getMembers().contains(newUser)) {
            throw new RuntimeException("User is already a member of the group.");
        }

        group.getMembers().add(newUser);
        chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent(usernameToAdd + " has been added to the group by " + requesterUsername + ".");
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
    }

    public List<ChatGroup> getGroupsForUser(String username) {
        return chatGroupRepository.findByMembersUsername(username);
    }

    public List<User> getGroupMembers(Long groupId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return List.copyOf(group.getMembers());
    }

    public List<GroupMessage> getGroupMessageHistory(Long groupId, String username) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!group.getMembers().contains(user)) {
            throw new RuntimeException("You are not a member of this group");
        }

        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);

    }

    public void leaveGroup(String username, Long groupId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!group.getMembers().contains(user)) {
            throw new RuntimeException("You are not a member of this group");
        }

        group.getMembers().remove(user);

        if (group.getMembers().isEmpty()) {
            chatGroupRepository.delete(group);
            return;
        }

        if (group.getOwner().equals(user)) {
            group.setOwner(group.getMembers().iterator().next());
        }

        chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent(username + " has left the group.");
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
    }

    public void kickMember(String requesterUsername, Long groupId, String usernameToKick) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        if (!group.getOwner().equals(requester)) {
            throw new RuntimeException("Only the group owner can kick members.");
        }

        User toKick = userRepository.findByUsername(usernameToKick)
                .orElseThrow(() -> new RuntimeException("User to kick not found"));

        if (!group.getMembers().contains(toKick)) {
            throw new RuntimeException("User is not a member of this group");
        }

        if (group.getOwner().equals(toKick)) {
            throw new RuntimeException("You cannot kick the group owner");
        }

        group.getMembers().remove(toKick);
        chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent(usernameToKick + " has been removed from the group by the owner.");
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
    }

    public void renameGroup(String requesterUsername, Long groupId, String newName) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        if (!group.getOwner().equals(requester)) {
            throw new RuntimeException("Only the group owner can rename the group.");
        }

        group.setName(newName);
        chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent("Group name changed to: " + newName);
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
    }

    public void transferOwnership(String requesterUsername, Long groupId, String newOwnerUsername) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        if (!group.getOwner().equals(requester)) {
            throw new RuntimeException("Only the group owner can transfer ownership.");
        }

        User newOwner = userRepository.findByUsername(newOwnerUsername)
                .orElseThrow(() -> new RuntimeException("New owner not found"));

        if (!group.getMembers().contains(newOwner)) {
            throw new RuntimeException("New owner must be a member of the group.");
        }

        group.setOwner(newOwner);
        chatGroupRepository.save(group);

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent("Ownership transferred to: " + newOwnerUsername);
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
    }

    @Transactional
    public void deleteGroup(String username, Long groupId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!group.getOwner().equals(user)) {
            throw new RuntimeException("Only the owner can delete the group");
        }

        GroupMessage systemMessage = new GroupMessage();
        systemMessage.setSender("SYSTEM");
        systemMessage.setContent("Group has been deleted by the owner.");
        systemMessage.setTimestamp(LocalDateTime.now());
        systemMessage.setGroup(group);

        groupMessageRepository.save(systemMessage);

        messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);

        chatGroupRepository.delete(group);
    }

}

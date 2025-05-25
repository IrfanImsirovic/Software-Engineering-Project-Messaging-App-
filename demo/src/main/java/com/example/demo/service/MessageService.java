package com.example.demo.service;

import com.example.demo.dto.ChatMessage;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SimpleXorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Encrypts the chatMessage content using SimpleXorUtil,
     * then saves via setters (no Lombok builder).
     */
    public Message sendMessage(String senderUsername, ChatMessage chatMessage) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUsername(chatMessage.getReceiverUsername())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!sender.getFriends().contains(receiver)) {
            throw new RuntimeException("You are not friends with this user.");
        }

        // encrypt the text content
        String encryptedContent = SimpleXorUtil.encrypt(chatMessage.getContent());

        // construct Message via setters
        Message message = new Message();
        message.setSender(sender.getUsername());
        message.setReceiver(receiver.getUsername());
        message.setContent(encryptedContent);
        message.setTimestamp(LocalDateTime.now());

        // if ChatMessage has an imageUrl field, uncomment below:
        // message.setImageUrl(chatMessage.getImageUrl());

        return messageRepository.save(message);
    }
}

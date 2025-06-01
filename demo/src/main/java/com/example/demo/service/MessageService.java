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

    public Message sendMessage(String senderUsername, ChatMessage chatMessage) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUsername(chatMessage.getReceiverUsername())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!sender.getFriends().contains(receiver)) {
            throw new RuntimeException("You are not friends with this user.");
        }

        String encryptedContent = SimpleXorUtil.encrypt(chatMessage.getContent());

        Message message = new Message();
        message.setSender(sender.getUsername());
        message.setReceiver(receiver.getUsername());
        message.setContent(encryptedContent);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }
}

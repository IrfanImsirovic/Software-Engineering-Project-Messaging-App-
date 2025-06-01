package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;

    // allow arbitrarily large Base64-ciphertext
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime timestamp;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ChatGroup group;

    public GroupMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ChatGroup getGroup() {
        return group;
    }

    public void setGroup(ChatGroup group) {
        this.group = group;
    }
}

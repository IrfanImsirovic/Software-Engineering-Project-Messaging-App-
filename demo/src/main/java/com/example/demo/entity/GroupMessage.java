package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
@Getter
@Setter
@NoArgsConstructor
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
}

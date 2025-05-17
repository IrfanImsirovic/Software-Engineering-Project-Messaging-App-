package com.example.demo.dto;

import lombok.Data;

@Data
public class GroupMessageDTO {
    private Long groupId;
    private String content;
    private String imageUrl;
}

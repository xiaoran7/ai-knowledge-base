package com.ai.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {
    private String id;
    private String name;
    private String description;
    private Integer documentCount;
    private LocalDateTime createdAt;
}
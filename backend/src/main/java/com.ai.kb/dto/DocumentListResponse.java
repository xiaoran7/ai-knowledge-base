package com.ai.kb.dto;

import java.util.List;

public record DocumentListResponse(
    List<DocumentResponse> list,
    long total,
    int page,
    int size
) {}
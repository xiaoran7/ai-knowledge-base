package com.ai.kb.controller;

import com.ai.kb.dto.ChatRequest;
import com.ai.kb.dto.ChatResponse;
import com.ai.kb.dto.RetrievalDebugRequest;
import com.ai.kb.dto.RetrievalDebugResponse;
import com.ai.kb.service.ChatService;
import com.ai.kb.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * AI 问答
     */
    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return chatService.chat(request, userId);
    }

    @PostMapping("/retrieval-debug")
    public RetrievalDebugResponse debugRetrieval(
            @RequestBody RetrievalDebugRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return chatService.debugRetrieval(request, userId);
    }

    /**
     * 从 Token 中提取用户 ID
     */
    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}

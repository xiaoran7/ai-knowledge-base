package com.ai.kb.controller;

import com.ai.kb.dto.ConversationDetailResponse;
import com.ai.kb.dto.ConversationMemoryUpdateRequest;
import com.ai.kb.dto.ConversationResponse;
import com.ai.kb.security.JwtTokenProvider;
import com.ai.kb.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public List<ConversationResponse> getList(
            @RequestParam("knowledgeBaseId") String kbId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.getConversationList(getUserIdFromToken(authHeader), kbId, page, size);
    }

    @GetMapping("/{id}")
    public ConversationDetailResponse getDetail(@PathVariable String id) {
        return chatService.getConversationDetail(id);
    }

    @PutMapping("/{id}/memory")
    public ConversationDetailResponse updateMemory(
            @PathVariable String id,
            @RequestBody ConversationMemoryUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return chatService.updateConversationMemory(id, request, getUserIdFromToken(authHeader));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportConversation(
            @PathVariable String id,
            @RequestParam(value = "format", defaultValue = "markdown") String format) {
        ChatService.ExportPayload payload = chatService.exportConversation(id, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        chatService.deleteConversation(id, getUserIdFromToken(authHeader));
        return Map.of("message", "删除成功");
    }

    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        return jwtTokenProvider.getUserIdFromToken(authHeader.substring(7));
    }
}

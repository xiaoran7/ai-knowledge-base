package com.ai.kb.controller;

import com.ai.kb.dto.DocumentListResponse;
import com.ai.kb.dto.DocumentResponse;
import com.ai.kb.dto.DocumentSummaryRequest;
import com.ai.kb.dto.DocumentTaskListResponse;
import com.ai.kb.dto.DocumentTaskResponse;
import com.ai.kb.dto.DocumentUpdateRequest;
import com.ai.kb.dto.DocumentUploadResponse;
import com.ai.kb.security.JwtTokenProvider;
import com.ai.kb.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/upload")
    public DocumentUploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") String kbId,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.upload(file, kbId, categoryId, getUserIdFromToken(authHeader));
    }

    @GetMapping
    public DocumentListResponse getList(
            @RequestParam("knowledgeBaseId") String kbId,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return documentService.getList(kbId, categoryId, page, size);
    }

    @GetMapping("/tasks")
    public DocumentTaskListResponse getTasks(
            @RequestParam("knowledgeBaseId") String kbId,
            @RequestParam(value = "documentId", required = false) String documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.getTasks(kbId, documentId, getUserIdFromToken(authHeader));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public DocumentTaskResponse cancelTask(
            @PathVariable String taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.cancelTask(taskId, getUserIdFromToken(authHeader));
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@PathVariable String id) {
        return documentService.getById(id);
    }

    @PutMapping("/{id}")
    public DocumentResponse update(
            @PathVariable String id,
            @RequestBody DocumentUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.updateContent(id, request, getUserIdFromToken(authHeader));
    }

    @PostMapping("/{id}/summary")
    public DocumentResponse generateSummary(
            @PathVariable String id,
            @RequestBody DocumentSummaryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.requestSummaryGeneration(id, request, getUserIdFromToken(authHeader));
    }

    @PostMapping("/{id}/retry")
    public DocumentResponse retry(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return documentService.retryProcessing(id, getUserIdFromToken(authHeader));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        documentService.delete(id, getUserIdFromToken(authHeader));
        return Map.of("message", "删除成功");
    }

    @PutMapping("/{id}/category")
    public Map<String, String> setCategory(
            @PathVariable String id,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        documentService.setCategory(id, categoryId, getUserIdFromToken(authHeader));
        return Map.of("message", "设置成功");
    }

    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        return jwtTokenProvider.getUserIdFromToken(authHeader.substring(7));
    }
}

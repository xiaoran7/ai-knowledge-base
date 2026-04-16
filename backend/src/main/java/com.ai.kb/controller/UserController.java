package com.ai.kb.controller;

import com.ai.kb.dto.UpdateUserProfileRequest;
import com.ai.kb.dto.UserProfileResponse;
import com.ai.kb.security.JwtTokenProvider;
import com.ai.kb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final String AVATAR_DIR = System.getProperty("user.dir") + "/uploads/avatars";

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return authService.getCurrentUser(getUserIdFromToken(authHeader));
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUser(
            @RequestBody UpdateUserProfileRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return authService.updateCurrentUser(getUserIdFromToken(authHeader), request);
    }

    @PostMapping("/me/avatar")
    public UserProfileResponse uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws IOException {
        return authService.uploadAvatar(getUserIdFromToken(authHeader), file);
    }

    @PostMapping("/me/assistant-avatar")
    public UserProfileResponse uploadAssistantAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws IOException {
        return authService.uploadAssistantAvatar(getUserIdFromToken(authHeader), file);
    }

    @GetMapping("/avatar/{userId}/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(
            @PathVariable String userId,
            @PathVariable String filename) throws IOException {
        Path avatarPath = Paths.get(AVATAR_DIR, userId, filename).normalize();
        if (!Files.exists(avatarPath) || !avatarPath.startsWith(Paths.get(AVATAR_DIR).normalize())) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(avatarPath.toUri());
        String contentType = Files.probeContentType(avatarPath);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}

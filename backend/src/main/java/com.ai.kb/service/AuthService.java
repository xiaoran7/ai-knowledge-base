package com.ai.kb.service;

import com.ai.kb.dto.AuthResponse;
import com.ai.kb.dto.LoginRequest;
import com.ai.kb.dto.RegisterRequest;
import com.ai.kb.dto.UpdateUserProfileRequest;
import com.ai.kb.dto.UserProfileResponse;
import com.ai.kb.entity.User;
import com.ai.kb.repository.UserRepository;
import com.ai.kb.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String AVATAR_DIR = System.getProperty("user.dir") + "/uploads/avatars";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");

        return generateAuthResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        return generateAuthResponse(user);
    }

    public void logout() {
        // JWT is stateless; frontend token removal is enough for now.
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token 无效");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return generateAuthResponse(user);
    }

    public UserProfileResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String avatarUrl = normalizeOptional(request.avatarUrl());
        String assistantAvatarUrl = normalizeOptional(request.assistantAvatarUrl());
        String bio = normalizeOptional(request.bio());

        if (avatarUrl != null && avatarUrl.length() > 500) {
            throw new IllegalArgumentException("头像地址过长");
        }
        if (assistantAvatarUrl != null && assistantAvatarUrl.length() > 500) {
            throw new IllegalArgumentException("AI 头像地址过长");
        }
        if (bio != null && bio.length() > 1000) {
            throw new IllegalArgumentException("个人简介超过长度限制 1000 字");
        }

        user.setAvatarUrl(avatarUrl);
        user.setAssistantAvatarUrl(assistantAvatarUrl);
        user.setBio(bio);

        return toUserProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse uploadAvatar(String userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        validateAvatarFile(file, "头像");
        String avatarUrl = saveAvatarFile(userId, file, user.getAvatarUrl(), null);
        user.setAvatarUrl(avatarUrl);
        return toUserProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse uploadAssistantAvatar(String userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        validateAvatarFile(file, "AI 头像");
        String avatarUrl = saveAvatarFile(userId, file, user.getAssistantAvatarUrl(), "assistant-");
        user.setAssistantAvatarUrl(avatarUrl);
        return toUserProfileResponse(userRepository.save(user));
    }

    private void validateAvatarFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + "文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException(label + "文件不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(label + "仅支持图片文件");
        }
    }

    private String saveAvatarFile(String userId, MultipartFile file, String oldAvatarUrl, String filenamePrefix)
            throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension.isBlank()) {
            extension = ".png";
        }

        Path avatarDir = Paths.get(AVATAR_DIR, userId).normalize();
        Files.createDirectories(avatarDir);

        deleteCurrentAvatarIfManaged(oldAvatarUrl, userId);

        String prefix = filenamePrefix == null ? "" : filenamePrefix;
        String filename = prefix + UUID.randomUUID() + extension;
        Path target = avatarDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/user/avatar/" + userId + "/" + filename;
    }

    private void deleteCurrentAvatarIfManaged(String avatarUrl, String userId) {
        if (avatarUrl == null || !avatarUrl.startsWith("/user/avatar/" + userId + "/")) {
            return;
        }
        String filename = avatarUrl.substring(("/user/avatar/" + userId + "/").length());
        if (filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(AVATAR_DIR, userId, filename));
        } catch (IOException ignored) {
            // Ignore cleanup failure to avoid blocking avatar replacement.
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return filename.substring(index).toLowerCase();
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getAssistantAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
        long expiration = 36000000L;

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getAssistantAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString()
        );

        return new AuthResponse(accessToken, refreshToken, expiration, userInfo);
    }
}

package com.ai.kb.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String userId,
        String username,
        String email,
        String avatarUrl,
        String assistantAvatarUrl,
        String bio,
        String role,
        LocalDateTime createdAt
) {
}

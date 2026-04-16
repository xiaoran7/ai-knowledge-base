package com.ai.kb.dto;

public record UpdateUserProfileRequest(
        String avatarUrl,
        String assistantAvatarUrl,
        String bio
) {
}

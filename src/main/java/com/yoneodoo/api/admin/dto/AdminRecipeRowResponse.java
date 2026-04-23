package com.yoneodoo.api.admin.dto;

import java.time.LocalDateTime;

public record AdminRecipeRowResponse(
        Long id,
        String title,
        String status,
        String videoId,
        String youtuberName,
        LocalDateTime createdAt
) {
}

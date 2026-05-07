package com.yoneodoo.api.admin.dto;

import java.time.LocalDateTime;

/**
 * 어드민 "레시피 목록" 테이블 한 줄에 필요한 필드만 담은 가벼운 응답입니다.
 * <p>
 * jsonb 재료 배열·긴 자막 텍스트 등 무거운 컬럼은 넣지 않아, 목록 화면 로딩을 가볍게 유지합니다.
 */
public record AdminRecipeRowResponse(
        Long id,
        String title,
        String status,
        String videoId,
        String youtuberName,
        LocalDateTime createdAt
) {
}

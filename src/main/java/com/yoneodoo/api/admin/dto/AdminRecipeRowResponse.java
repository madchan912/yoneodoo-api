package com.yoneodoo.api.admin.dto;

import com.yoneodoo.api.entity.DisplayStatus;

import java.time.LocalDateTime;

/**
 * 어드민 "레시피 목록" 테이블 한 줄에 필요한 필드만 담은 가벼운 응답입니다.
 * <p>
 * jsonb 재료 배열·긴 자막 텍스트 등 무거운 컬럼은 넣지 않아, 목록 화면 로딩을 가볍게 유지합니다.
 *
 * @param displayStatus 사용자 노출 여부(ACTIVE/HIDDEN). 어드민 화면에서 뱃지로 표시.
 */
public record AdminRecipeRowResponse(
        Long id,
        String title,
        String status,
        DisplayStatus displayStatus,
        String videoId,
        String youtuberName,
        LocalDateTime createdAt
) {
}

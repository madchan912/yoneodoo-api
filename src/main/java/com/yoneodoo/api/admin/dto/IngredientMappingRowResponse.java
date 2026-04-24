package com.yoneodoo.api.admin.dto;

import java.time.LocalDateTime;

/** 매핑 완료된 원본 재료명 + 마스터명 + 등록 시각(최신순 정렬용). */
public record IngredientMappingRowResponse(
        String rawName,
        String masterName,
        LocalDateTime createdAt
) {
}

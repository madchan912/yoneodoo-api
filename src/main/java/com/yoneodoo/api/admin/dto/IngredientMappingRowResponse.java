package com.yoneodoo.api.admin.dto;

import java.time.LocalDateTime;

/**
 * 이미 {@code ingredient_mapping} 테이블에 올라간 한 줄을 API로 내려줄 때 쓰는 응답 형태입니다.
 *
 * @param rawName     정규화된 원본 재료 키
 * @param masterName  묶인 표준 재료명
 * @param createdAt   매핑이 처음 저장된 시각(최신순 정렬에 사용)
 */
public record IngredientMappingRowResponse(
        String rawName,
        String masterName,
        LocalDateTime createdAt
) {
}

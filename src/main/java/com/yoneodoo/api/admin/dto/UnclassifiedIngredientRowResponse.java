package com.yoneodoo.api.admin.dto;

/** 미분류(매핑 테이블에 없는) 원본 재료명 + 레시피 JSON 내 출현 횟수. */
public record UnclassifiedIngredientRowResponse(
        String rawName,
        long occurrenceCount
) {
}

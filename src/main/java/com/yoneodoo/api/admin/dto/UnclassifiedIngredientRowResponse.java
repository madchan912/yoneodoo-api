package com.yoneodoo.api.admin.dto;

/**
 * 추후 마스터 재료 큐와 연동할 때 사용할 스키마 껍데기.
 */
public record UnclassifiedIngredientRowResponse(
        String rawName,
        long occurrenceCount
) {
}

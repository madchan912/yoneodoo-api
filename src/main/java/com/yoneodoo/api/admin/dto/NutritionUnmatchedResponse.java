package com.yoneodoo.api.admin.dto;

/**
 * 영양성분 미매칭 재료 목록 응답 DTO.
 * GET /api/v1/admin/nutrition/unmatched 에서 사용합니다.
 *
 * @param id         ingredient_nutrition 행 ID
 * @param masterName 표준 재료명(ingredient_mapping.master_name과 동일 키)
 * @param source     현재 출처 값(manual_needed / foodsafety_kr / manual)
 */
public record NutritionUnmatchedResponse(Long id, String masterName, String source) {
}

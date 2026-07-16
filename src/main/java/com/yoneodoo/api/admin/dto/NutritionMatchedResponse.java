package com.yoneodoo.api.admin.dto;

import java.math.BigDecimal;

/**
 * 영양성분 적재 완료 재료 목록 응답 DTO.
 * GET /api/v1/admin/nutrition/matched 에서 사용합니다.
 * source != 'manual_needed'인 항목 전체 — 어드민 완료 탭 표시 및 수정에 사용됩니다.
 *
 * @param id           ingredient_nutrition 행 ID
 * @param masterName   표준 재료명
 * @param calories     열량(kcal/100g)
 * @param protein      단백질(g/100g)
 * @param fat          지방(g/100g)
 * @param saturatedFat 포화지방(g/100g)
 * @param carbohydrate 탄수화물(g/100g)
 * @param sugar        당류(g/100g)
 * @param sodium       나트륨(mg/100g)
 * @param source       출처(foodsafety_kr / manual / gemini_est)
 */
public record NutritionMatchedResponse(
        Long id,
        String masterName,
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal saturatedFat,
        BigDecimal carbohydrate,
        BigDecimal sugar,
        BigDecimal sodium,
        String source
) {
}

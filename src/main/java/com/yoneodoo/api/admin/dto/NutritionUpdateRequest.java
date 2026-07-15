package com.yoneodoo.api.admin.dto;

import java.math.BigDecimal;

/**
 * 영양성분 수동 저장 요청 DTO.
 * PUT /api/v1/admin/nutrition/{masterName} 에서 사용합니다.
 *
 * @param calories     열량(kcal/100g)
 * @param protein      단백질(g/100g)
 * @param fat          지방(g/100g)
 * @param saturatedFat 포화지방(g/100g)
 * @param carbohydrate 탄수화물(g/100g)
 * @param sugar        당류(g/100g)
 * @param sodium       나트륨(mg/100g)
 * @param source       출처 값(manual_needed / foodsafety_kr / manual)
 */
public record NutritionUpdateRequest(
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

package com.yoneodoo.api.admin.dto;

import java.math.BigDecimal;

/**
 * 식품성분표 검색 결과 응답 DTO.
 * GET /api/v1/admin/nutrition/search?keyword= 에서 반환합니다.
 * 어드민이 원하는 항목을 선택하면 영양 값이 폼에 자동으로 채워집니다.
 *
 * @param id          food_nutrition_master 행 ID
 * @param foodName    식품성분표 원본 식품명(예: "닭고기, 가슴, 생것")
 * @param foodGroup   식품군(예: "육류")
 * @param calories    열량(kcal/100g)
 * @param protein     단백질(g/100g)
 * @param fat         지방(g/100g)
 * @param saturatedFat 포화지방(g/100g)
 * @param carbohydrate 탄수화물(g/100g)
 * @param sugar       당류(g/100g)
 * @param sodium      나트륨(mg/100g)
 */
public record FoodSearchResponse(
        Long id,
        String foodName,
        String foodGroup,
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal saturatedFat,
        BigDecimal carbohydrate,
        BigDecimal sugar,
        BigDecimal sodium
) {
}

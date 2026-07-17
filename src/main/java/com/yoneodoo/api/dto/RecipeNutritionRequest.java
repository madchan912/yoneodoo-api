package com.yoneodoo.api.dto;

import java.math.BigDecimal;

/**
 * {@code POST /api/v1/recipes/{id}/nutrition} 요청 본문.
 * yoneodoo-data 파이프라인이 계산한 영양 합계를 전송합니다.
 */
public record RecipeNutritionRequest(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal saturatedFat,
        BigDecimal carbohydrate,
        BigDecimal sugar,
        BigDecimal sodium,
        BigDecimal coveragePct
) {}

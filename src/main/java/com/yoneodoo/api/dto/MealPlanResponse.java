package com.yoneodoo.api.dto;

import java.util.List;
import java.util.Map;

/**
 * RAG 식단 플래너 응답 DTO입니다.
 *
 * @param mealPlan   Gemini가 생성한 N일 식단 텍스트
 * @param recipes    후보 레시피 목록 (벡터 유사도 상위 20개)
 * @param conditions Gemini가 추출한 식단 조건 (max_calories, exclude_ingredients, goal, days)
 */
public record MealPlanResponse(
        String mealPlan,
        List<Map<String, Object>> recipes,
        Map<String, Object> conditions
) {}

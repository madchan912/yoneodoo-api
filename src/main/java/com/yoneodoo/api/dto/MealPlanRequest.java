package com.yoneodoo.api.dto;

/**
 * RAG 식단 플래너 요청 DTO입니다.
 * <p>
 * 사용자가 자연어로 식단 조건을 입력하면, 서비스가 Gemini로 조건을 추출하고
 * 벡터 검색 → 식단 조합 순서로 처리합니다.
 */
public record MealPlanRequest(
        /** 사용자 자연어 입력 예: "일주일 다이어트 식단, 두부 알레르기 있어" */
        String query
) {}

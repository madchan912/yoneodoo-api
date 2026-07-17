package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.MealPlanRequest;
import com.yoneodoo.api.dto.MealPlanResponse;
import com.yoneodoo.api.service.RecipeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 기반 식단 검색 API 진입점입니다.
 * <p>
 * 인증 없이 공개 — 사용자 자연어 입력을 받아
 * {@link RecipeSearchService}가 Gemini + pgvector 파이프라인으로 식단을 생성합니다.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class RecipeSearchController {

    private final RecipeSearchService recipeSearchService;

    /**
     * 자연어 쿼리로 식단 플랜을 생성합니다.
     * <p>
     * ① 조건 추출 → ② 벡터화 → ③ pgvector 유사도 검색 → ④ Gemini 식단 조합
     *
     * @param request {@code { "query": "사용자 자연어 입력" }}
     * @return {@code meal_plan}(식단 텍스트), {@code recipes}(후보 레시피), {@code conditions}(추출 조건)
     */
    @PostMapping("/meal-plan")
    public MealPlanResponse mealPlan(@RequestBody MealPlanRequest request) {
        log.info("식단 플랜 요청: query={}", request.query());
        return recipeSearchService.search(request.query());
    }
}

package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.RecipeNutritionRequest;
import com.yoneodoo.api.entity.RecipeNutrition;
import com.yoneodoo.api.repository.RecipeNutritionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 레시피 영양성분 적재 API.
 * <p>
 * yoneodoo-data 파이프라인이 레시피 저장 직후 호출하는 내부 전용 엔드포인트입니다.
 * 인증 없이 사용하며, recipe_id 기준으로 upsert합니다.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeNutritionController {

    private final RecipeNutritionRepository recipeNutritionRepository;

    /**
     * 레시피 영양성분을 저장하거나 업데이트합니다 (upsert).
     * <p>
     * ① recipe_id로 기존 행을 조회합니다.<br>
     * ② 없으면 새 행을 INSERT, 있으면 영양 값만 UPDATE합니다.<br>
     * ③ 저장된 행의 id를 반환합니다.
     *
     * @param recipeId 대상 레시피 PK
     * @param req      영양 합계 값 묶음
     */
    @PostMapping("/{recipeId}/nutrition")
    @Transactional
    public ResponseEntity<String> upsertNutrition(
            @PathVariable Long recipeId,
            @RequestBody RecipeNutritionRequest req) {

        RecipeNutrition rn = recipeNutritionRepository.findByRecipeId(recipeId)
                .orElseGet(() -> RecipeNutrition.of(recipeId));

        rn.setCalories(req.calories());
        rn.setProtein(req.protein());
        rn.setFat(req.fat());
        rn.setSaturatedFat(req.saturatedFat());
        rn.setCarbohydrate(req.carbohydrate());
        rn.setSugar(req.sugar());
        rn.setSodium(req.sodium());
        rn.setCoveragePct(req.coveragePct());
        rn.setUpdatedAt(LocalDateTime.now());

        RecipeNutrition saved = recipeNutritionRepository.save(rn);
        return ResponseEntity.ok("nutrition saved: id=" + saved.getId());
    }
}

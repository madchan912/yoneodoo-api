package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.RecipeCreateRequest;
import com.yoneodoo.api.entity.DisplayStatus;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 레시피 데이터에 대한 HTTP API 진입점입니다.
 * <p>
 * <b>역할 분리</b><br>
 * · {@link #getAllRecipes()}: 단순 조회는 리포지토리를 직접 호출(전체 목록이 필요한 화면/도구용).<br>
 * · {@link #createRecipe(RecipeCreateRequest)}: 저장은 반드시 {@link RecipeService}를 거쳐
 * 재료명 정리 같은 비즈니스 규칙이 적용되게 합니다.
 * <p>
 * <b>데이터가 들어오는 대표 경로</b><br>
 * 크롤러(파이썬) → POST 본문(JSON) → 이 컨트롤러 → 서비스 → DB {@code recipes}.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecipeController {

    /** 조회 전용으로 쓰는 레시피 저장소(필터 없이 전체). */
    private final RecipeRepository recipeRepository;
    /** 레시피 저장 시 도메인 규칙을 적용하는 서비스. */
    private final RecipeService recipeService;

    /**
     * 사용자에게 노출 가능한({@link DisplayStatus#ACTIVE}) 레시피만 반환합니다.
     * <p>
     * Soft Delete된 ({@code HIDDEN}) 레시피는 절대로 응답에 포함되지 않습니다.
     * 어드민 화면이 전체를 보고 싶으면 {@code /api/v1/admin/recipes}를 사용해야 합니다.
     */
    @GetMapping
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findByDisplayStatus(DisplayStatus.ACTIVE);
    }

    /**
     * 외부 시스템이 새 레시피 한 건을 적재할 때 호출하는 API입니다.
     * <p>
     * 본문은 {@link RecipeCreateRequest} 형식이며, 서비스에서 재료명 정리 후 {@code recipes} 행으로 저장됩니다.
     */
    @PostMapping
    public ResponseEntity<String> createRecipe(@RequestBody RecipeCreateRequest request) {
        recipeService.saveRecipe(request);
        return ResponseEntity.ok("레시피 저장 및 캐시 업데이트 완료!");
    }
}

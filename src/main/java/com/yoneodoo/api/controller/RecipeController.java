package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.RecipeCreateRequest;
import com.yoneodoo.api.dto.RecipeIngredientData;
import com.yoneodoo.api.dto.RecipeResponse;
import com.yoneodoo.api.entity.DisplayStatus;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.service.IngredientSearchService;
import com.yoneodoo.api.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 레시피 데이터에 대한 HTTP API 진입점입니다.
 * <p>
 * <b>역할 분리</b><br>
 * · {@link #getAllRecipes()}: 조회는 리포지토리를 직접 호출 후 {@link RecipeResponse}로 변환.<br>
 * · {@link #createRecipe(RecipeCreateRequest)}: 저장은 반드시 {@link RecipeService}를 거쳐
 * 재료명 정리 같은 비즈니스 규칙이 적용되게 합니다.
 * <p>
 * CORS 정책은 {@link com.yoneodoo.api.config.CorsConfig}에서 일괄 관리합니다.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    /** 조회 전용으로 쓰는 레시피 저장소. */
    private final RecipeRepository recipeRepository;
    /** 레시피 저장 시 도메인 규칙을 적용하는 서비스. */
    private final RecipeService recipeService;
    /** raw 재료명 → master_name 변환에 사용하는 캐시 서비스. */
    private final IngredientSearchService ingredientSearchService;

    /**
     * 사용자에게 노출 가능한 레시피만 반환합니다(이중 안전장치).
     * <p>
     * <b>두 조건을 모두 만족하는 레시피만</b> 응답합니다:<br>
     * · 파이프라인 처리 성공: {@code status = "SUCCESS"}<br>
     * · 어드민 노출 허용: {@code displayStatus = ACTIVE}
     * <p>
     * 응답 DTO({@link RecipeResponse})에는 내부 파이프라인 필드({@code status}, {@code displayStatus},
     * {@code transcript})가 포함되지 않습니다.
     */
    @GetMapping
    public List<RecipeResponse> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findByStatusAndDisplayStatus(Recipe.STATUS_SUCCESS, DisplayStatus.ACTIVE);
        return toResponse(recipes);
    }

    /**
     * 요리명 키워드로 레시피를 검색합니다 (사용자용 요리명 검색 모드).
     * <p>
     * ① {@code q}가 비어 있으면 빈 목록을 즉시 반환합니다.<br>
     * ② 키워드를 리포지토리에 전달해 ILIKE 검색 결과를 {@link RecipeResponse}로 변환 후 반환합니다.
     *
     * @param q 사용자 입력 요리명 검색어
     * @return 제목에 키워드가 포함된 레시피 목록
     */
    @GetMapping("/search")
    public List<RecipeResponse> searchRecipes(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) {
            return List.of();
        }
        return toResponse(recipeRepository.searchByTitle(q));
    }

    /**
     * {@link Recipe} 엔티티 목록을 {@link RecipeResponse} DTO 목록으로 변환합니다.
     * <p>
     * 각 재료명은 {@link IngredientSearchService#toMaster(String)}으로 master_name으로 교체됩니다.
     * 새 {@link RecipeIngredientData} 객체를 만들어 원본 엔티티를 건드리지 않습니다.
     */
    private List<RecipeResponse> toResponse(List<Recipe> recipes) {
        return recipes.stream()
                .map(recipe -> {
                    List<RecipeIngredientData> masterIngredients = recipe.getIngredients() == null
                            ? List.of()
                            : recipe.getIngredients().stream()
                                    .map(ing -> new RecipeIngredientData(
                                            ingredientSearchService.toMaster(ing.getName()),
                                            ing.getAmount()))
                                    .collect(Collectors.toList());
                    return RecipeResponse.from(recipe, masterIngredients);
                })
                .collect(Collectors.toList());
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

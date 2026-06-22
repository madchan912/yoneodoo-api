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
     * 사용자에게 노출 가능한 레시피만 반환합니다(이중 안전장치).
     * <p>
     * <b>두 조건을 모두 만족하는 레시피만</b> 응답합니다:<br>
     * · 파이프라인 처리 성공: {@code status = "SUCCESS"} — 자막 없음/스킵 등 비정상 적재 차단<br>
     * · 어드민 노출 허용: {@code displayStatus = ACTIVE} — Soft Delete 된 행 차단
     * <p>
     * 둘 중 하나라도 어긋나면 사용자 화면에서 보이지 않습니다.
     * 어드민 전체 목록은 {@code /api/v1/admin/recipes}를 사용해야 합니다.
     */
    @GetMapping
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findByStatusAndDisplayStatus(Recipe.STATUS_SUCCESS, DisplayStatus.ACTIVE);
    }

    /**
     * 요리명 키워드로 레시피를 검색합니다 (사용자용 요리명 검색 모드).
     * <p>
     * ① {@code q}가 비어 있으면 빈 목록을 즉시 반환합니다.<br>
     * ② 키워드를 리포지토리에 전달해 ILIKE 검색 결과를 반환합니다.
     * 노출 안전장치(status/displayStatus)는 쿼리 안에서 적용됩니다.
     *
     * @param q 사용자 입력 요리명 검색어
     * @return 제목에 키워드가 포함된 레시피 목록
     */
    @GetMapping("/search")
    public List<Recipe> searchRecipes(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) {
            return List.of();
        }
        return recipeRepository.searchByTitle(q);
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

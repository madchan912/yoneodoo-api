package com.yoneodoo.api.controller;

import com.yoneodoo.api.service.IngredientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 재료 "검색·자동완성" API의 진입점입니다.
 * <p>
 * <b>데이터의 출처</b><br>
 * 실제 검색은 DB를 직접 두드리지 않고, 서버 메모리에 올려 둔 캐시({@link IngredientSearchService})에서 수행합니다.
 * 캐시 내용은 모든 레시피의 JSON 재료명을 모아 만든 집합입니다.
 * <p>
 * <b>캐시가 언제 갱신되나</b><br>
 * 서버 기동 시 + 어드민에서 재료 매핑을 저장/삭제한 직후(서비스에서 {@code initCache} 호출).
 */
@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IngredientController {

    private final IngredientSearchService ingredientSearchService;

    /**
     * 키워드로 재료 후보를 검색합니다.
     *
     * @param keyword 사용자 입력(빈 문자열이면 빈 배열 반환)
     * @return 최대 20개의 후보({@link IngredientSearchService.IngredientCacheDto})
     */
    @GetMapping("/search")
    public List<IngredientSearchService.IngredientCacheDto> searchIngredients(
            @RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return ingredientSearchService.search(keyword);
    }
}

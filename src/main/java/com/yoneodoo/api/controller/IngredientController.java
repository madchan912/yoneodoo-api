package com.yoneodoo.api.controller;

import com.yoneodoo.api.service.IngredientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientSearchService ingredientSearchService;

    // 프론트에서 GET /api/v1/ingredients/search?keyword=ㄱㅈ 형태로 찔러볼 주소!
    @GetMapping("/search")
    public List<IngredientSearchService.IngredientCacheDto> searchIngredients(
            @RequestParam(name = "keyword", defaultValue = "") String keyword) {

        // 우리가 짜둔 기가 막힌 인메모리 검색 로직 호출!
        return ingredientSearchService.search(keyword);
    }
}
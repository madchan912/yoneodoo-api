package com.yoneodoo.api.controller;

import com.yoneodoo.api.service.IngredientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin; // 🚀 이거 임포트 추가!
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 🚀 [핵심] 리액트의 접근을 허락해 줍니다!
public class IngredientController {

    private final IngredientSearchService ingredientSearchService;

    @GetMapping("/search")
    public List<IngredientSearchService.IngredientCacheDto> searchIngredients(
            @RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return ingredientSearchService.search(keyword);
    }
}
package com.yoneodoo.api.controller;

import com.yoneodoo.api.dto.RecipeCreateRequest; // (다음 스텝에서 만들 DTO)
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.service.RecipeService; // (다음 스텝에서 만들 Service)
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService; // 🚀 POST 저장을 담당할 서비스 추가

    // 기존 로직: 레시피 전체 조회 (GET /api/v1/recipes)
    @GetMapping
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    // 🚀 추가된 로직: 파이썬 크롤러가 던진 새 레시피 저장 (POST /api/v1/recipes)
    @PostMapping
    public ResponseEntity<String> createRecipe(@RequestBody RecipeCreateRequest request) {
        recipeService.saveRecipe(request); // 서비스에게 데이터 정제 및 저장을 맡김
        return ResponseEntity.ok("레시피 저장 및 캐시 업데이트 완료!");
    }
}
package com.yoneodoo.api.controller;

import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes") // 다른 API들과 버전(v1) 깔맞춤!
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @GetMapping
    public List<Recipe> getAllRecipes() {
        // 일꾼(Repository)을 시켜서 파이썬이 넣은 데이터까지 싹 다 꺼내옵니다.
        return recipeRepository.findAll();
    }
}
package com.yoneodoo.api.controller;

import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 🚀 리액트(Vite 기본 포트)에서 호출할 수 있게 허락!
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @GetMapping
    public List<Recipe> getAllRecipes() {
        // 일꾼(Repository)을 시켜서 파이썬이 넣은 데이터까지 싹 다 꺼내옵니다.
        return recipeRepository.findAll();
    }
}
package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.RecipeNutrition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@code recipe_nutrition} 테이블 접근 계층.
 * recipe_id UNIQUE 제약 덕분에 단건 조회/upsert에 사용합니다.
 */
public interface RecipeNutritionRepository extends JpaRepository<RecipeNutrition, Integer> {

    Optional<RecipeNutrition> findByRecipeId(Long recipeId);
}

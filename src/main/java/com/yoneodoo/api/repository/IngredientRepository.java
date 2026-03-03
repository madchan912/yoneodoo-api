package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    // 이름으로 재료 찾기
    Optional<Ingredient> findByName(String name);
}
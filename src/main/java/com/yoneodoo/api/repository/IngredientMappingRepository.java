package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.IngredientMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientMappingRepository extends JpaRepository<IngredientMapping, Long> {

    Optional<IngredientMapping> findByRawName(String rawName);

    List<IngredientMapping> findAllByOrderByCreatedAtDesc();
}

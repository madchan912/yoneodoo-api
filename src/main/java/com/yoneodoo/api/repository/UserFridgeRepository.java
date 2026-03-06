package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.UserFridge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFridgeRepository extends JpaRepository<UserFridge, Long> {

    List<UserFridge> findByUserId(Long userId);

    void deleteByUserIdAndIngredientId(Long userId, Long ingredientId);
}
package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.FoodNutritionMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@code food_nutrition_master} 테이블 접근 계층입니다.
 * <p>
 * 식품성분표(10개정판) 전체 16,535건이 담긴 참조 테이블로,
 * 어드민이 수동 매칭할 때 키워드로 식품명을 검색하는 데 사용합니다.
 */
public interface FoodNutritionMasterRepository extends JpaRepository<FoodNutritionMaster, Long> {

    /**
     * 식품명에 키워드가 포함된 항목을 최대 20건 조회합니다.
     * 어드민이 "닭고기" 입력 시 "닭고기, 가슴, 생것" 등이 검색되도록 LIKE 처리합니다.
     */
    @Query("SELECT f FROM FoodNutritionMaster f WHERE LOWER(f.foodName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY f.foodName ASC")
    List<FoodNutritionMaster> searchByFoodName(@Param("keyword") String keyword, Pageable pageable);
}

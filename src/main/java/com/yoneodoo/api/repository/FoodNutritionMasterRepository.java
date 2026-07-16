package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.FoodNutritionMaster;
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
     * 식품명에 키워드가 포함된 항목을 식품명 기준 중복 제거 후 최대 20건 조회합니다.
     * 시트별(10.0~10.4) 동일 식품명 중복을 DISTINCT ON으로 제거하고, calories 내림차순으로 대표값 선택.
     * DISTINCT ON은 JPQL 미지원이므로 nativeQuery=true 사용.
     */
    @Query(value = """
            SELECT DISTINCT ON (food_name) *
            FROM food_nutrition_master
            WHERE food_name ILIKE CONCAT('%', :keyword, '%')
            ORDER BY food_name, calories DESC NULLS LAST
            LIMIT 20
            """, nativeQuery = true)
    List<FoodNutritionMaster> searchByFoodName(@Param("keyword") String keyword);
}

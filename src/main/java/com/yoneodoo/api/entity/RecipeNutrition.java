package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블 {@code recipe_nutrition}: 레시피 한 인분 기준 영양 합계입니다.
 * <p>
 * yoneodoo-data 파이프라인이 재료별 ingredient_nutrition을 unit 환산해 합산한 뒤
 * {@code POST /api/v1/recipes/{id}/nutrition}으로 적재합니다.
 * recipe_id 컬럼에 UNIQUE 제약이 있어 upsert 방식으로 사용됩니다.
 */
@Entity
@Table(
        name = "recipe_nutrition",
        uniqueConstraints = @UniqueConstraint(name = "uq_recipe_nutrition_recipe_id", columnNames = "recipe_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 레시피 FK. recipes.id 와 1:1 대응. */
    @Column(name = "recipe_id")
    private Long recipeId;

    /** 열량 합계 (kcal). */
    @Column(precision = 7, scale = 2)
    private BigDecimal calories;

    /** 단백질 합계 (g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal protein;

    /** 지방 합계 (g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal fat;

    /** 포화지방 합계 (g). */
    @Column(name = "saturated_fat", precision = 7, scale = 2)
    private BigDecimal saturatedFat;

    /** 탄수화물 합계 (g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal carbohydrate;

    /** 당류 합계 (g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal sugar;

    /** 나트륨 합계 (mg). */
    @Column(precision = 7, scale = 2)
    private BigDecimal sodium;

    /**
     * 재료 중 영양 데이터가 있는 비율 (0~100).
     * coverage_pct < 50이면 프론트엔드에서 칼로리를 미표시.
     */
    @Column(name = "coverage_pct", precision = 5, scale = 2)
    private BigDecimal coveragePct;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static RecipeNutrition of(Long recipeId) {
        RecipeNutrition rn = new RecipeNutrition();
        rn.recipeId = recipeId;
        rn.createdAt = LocalDateTime.now();
        rn.updatedAt = LocalDateTime.now();
        return rn;
    }
}

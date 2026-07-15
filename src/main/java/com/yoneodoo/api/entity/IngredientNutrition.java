package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블 {@code ingredient_nutrition}: ingredient_mapping의 마스터 재료명에 대한 영양성분 정보입니다.
 * <p>
 * insert_nutrition.py로 식품성분표(10개정판)와 자동 매칭해 적재하며,
 * 미매칭 항목({@code source='manual_needed'})은 어드민 화면에서 수동으로 채웁니다.
 * <p>
 * {@code master_name}은 {@code ingredient_mapping.master_name}과 동일 키입니다.
 * serving_size=100, serving_unit="g" 기준 데이터입니다.
 */
@Entity
@Table(
        name = "ingredient_nutrition",
        uniqueConstraints = @UniqueConstraint(name = "uq_ingredient_nutrition_master_name", columnNames = "master_name")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ingredient_mapping.master_name과 1:1 대응되는 표준 재료명 키.
     */
    @Column(name = "master_name", nullable = false, length = 100)
    private String masterName;

    /** 열량 (kcal / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal calories;

    /** 단백질 (g / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal protein;

    /** 지방 (g / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal fat;

    /** 포화지방 (g / 100g). */
    @Column(name = "saturated_fat", precision = 7, scale = 2)
    private BigDecimal saturatedFat;

    /** 탄수화물 (g / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal carbohydrate;

    /** 당류 (g / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal sugar;

    /** 나트륨 (mg / 100g). */
    @Column(precision = 7, scale = 2)
    private BigDecimal sodium;

    /** 기준 제공량(기본 100). */
    @Column(name = "serving_size", precision = 7, scale = 2)
    private BigDecimal servingSize;

    /** 기준 단위(기본 "g"). */
    @Column(name = "serving_unit", length = 20)
    private String servingUnit;

    /**
     * 데이터 출처.
     * <ul>
     *   <li>{@code foodsafety_kr}: 식품성분표(10개정판) 자동 매칭</li>
     *   <li>{@code manual_needed}: 미매칭 → 어드민 수동 입력 필요</li>
     *   <li>{@code manual}: 어드민이 수동으로 입력 완료</li>
     * </ul>
     */
    @Column(length = 50)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

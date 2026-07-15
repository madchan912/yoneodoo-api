package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블 {@code food_nutrition_master}: 식품성분표(10개정판) 전체 16,535건을 담는 참조 테이블입니다.
 * <p>
 * insert_food_master.py로 xlsx 5개 시트를 적재하며, 이 테이블은 읽기 전용 참조용입니다.
 * 어드민 영양성분 관리 화면에서 "식품 검색 → 선택 → ingredient_nutrition 업데이트" 흐름의 원천 데이터입니다.
 */
@Entity
@Table(
        name = "food_nutrition_master",
        indexes = @Index(name = "idx_food_nutrition_master_name", columnList = "food_name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodNutritionMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 식품성분표 원본 식품명(예: "닭고기, 가슴, 생것"). */
    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    /** 식품군(예: "육류"). */
    @Column(name = "food_group", length = 100)
    private String foodGroup;

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

    /** 출처 버전(Database 10.0~10.4 중 하나). */
    @Column(name = "source_ver", length = 20)
    private String sourceVer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

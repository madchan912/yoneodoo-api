package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 테이블 {@code rag_search_log}: RAG 식단 플래너 사용 이력을 남깁니다.
 * <p>
 * {@code RecipeSearchService.search()}가 식단 조합을 완료할 때마다 한 행씩 적재합니다.
 * {@code conditions}/{@code recipes}는 각각 Gemini가 추출한 조건, 후보 레시피 목록을 JSON 문자열로 보관합니다.
 * {@code userId}는 소셜 로그인 도입 전까지 항상 null입니다.
 */
@Entity
@Table(name = "rag_search_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 검색을 요청한 사용자 ID. 소셜 로그인 미구현 상태라 현재는 항상 null. */
    @Column(name = "user_id")
    private Long userId;

    /** 사용자가 입력한 자연어 쿼리 원문. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    /** Gemini가 추출한 식단 조건(max_calories, exclude_ingredients, goal, days)을 JSON 문자열로 저장. */
    @Column(columnDefinition = "jsonb")
    private String conditions;

    /** pgvector 유사도 검색으로 뽑힌 후보 레시피 목록을 JSON 문자열로 저장. */
    @Column(columnDefinition = "jsonb")
    private String recipes;

    /** Gemini가 생성한 N일 식단 텍스트. */
    @Column(name = "meal_plan", columnDefinition = "TEXT")
    private String mealPlan;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static RagSearchLog of(Long userId, String query, String conditions, String recipes, String mealPlan) {
        RagSearchLog log = new RagSearchLog();
        log.userId = userId;
        log.query = query;
        log.conditions = conditions;
        log.recipes = recipes;
        log.mealPlan = mealPlan;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}

package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.RecipeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * {@code recipe_embeddings} 테이블 접근 계층입니다.
 * <p>
 * <b>핵심 메서드 — upsertEmbedding</b><br>
 * JPA가 {@code vector} 타입을 직접 다룰 수 없어, 네이티브 SQL의 {@code CAST(:embedding AS vector)} 로
 * JSON 배열 문자열({@code "[0.1, 0.2, ...]"})을 pgvector 타입으로 변환해 삽입합니다.
 * {@code ?2::vector} 구문은 Hibernate가 {@code ParameterLabelException}을 발생시켜 named parameter로 변경.
 * ON CONFLICT DO UPDATE로 같은 recipe_id가 있으면 덮어씁니다.
 */
public interface RecipeEmbeddingRepository extends JpaRepository<RecipeEmbedding, Long> {

    /** recipe_id로 기존 임베딩 행 조회 (존재 여부 확인용). */
    Optional<RecipeEmbedding> findByRecipeId(Long recipeId);

    /**
     * 임베딩 upsert — recipe_id가 없으면 INSERT, 있으면 UPDATE.
     * <p>
     * {@code embeddingJson}: Jackson이 직렬화한 {@code "[0.1, 0.2, ...]"} 형태.
     * PostgreSQL이 {@code ::vector} 캐스트로 pgvector 타입으로 변환합니다.
     *
     * @param recipeId     레시피 기본키
     * @param embeddingJson 768차원 벡터를 JSON 배열로 직렬화한 문자열
     */
    @Modifying
    @Query(value = """
            INSERT INTO recipe_embeddings (recipe_id, embedding, updated_at)
            VALUES (:recipeId, CAST(:embedding AS vector), NOW())
            ON CONFLICT (recipe_id) DO UPDATE SET
                embedding  = EXCLUDED.embedding,
                updated_at = NOW()
            """, nativeQuery = true)
    void upsertEmbedding(@Param("recipeId") Long recipeId, @Param("embedding") String embeddingJson);

    /**
     * 쿼리 벡터와 코사인 유사도가 높은 레시피를 최대 20건 반환합니다.
     * <p>
     * 기획 관점: RAG 식단 플래너의 후보 레시피 풀 — 벡터 근접 순으로 뽑아
     * Gemini 식단 조합 프롬프트에 전달합니다.
     * <p>
     * 반환 컬럼 순서: id(0), title(1), video_id(2), youtuber_name(3),
     * calories(4), protein(5), coverage_pct(6), similarity(7)
     *
     * @param embedding   쿼리 벡터 JSON 배열 문자열 {@code "[0.1, ...]"}
     * @param maxCalories 칼로리 상한 (null이면 제한 없음)
     */
    @Query(value = """
            SELECT r.id, r.title, r.video_id, r.youtuber_name,
                   rn.calories, rn.protein, rn.coverage_pct,
                   1 - (re.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM recipe_embeddings re
            JOIN recipes r ON re.recipe_id = r.id
            JOIN recipe_nutrition rn ON r.id = rn.recipe_id
            WHERE r.status = 'SUCCESS'
              AND r.display_status = 'ACTIVE'
              AND rn.coverage_pct >= 50
              AND (:maxCalories IS NULL OR rn.calories <= :maxCalories)
            ORDER BY re.embedding <=> CAST(:embedding AS vector)
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> findSimilarRecipes(
            @Param("embedding") String embedding,
            @Param("maxCalories") Double maxCalories
    );
}

package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DB 테이블 {@code recipe_embeddings} 한 행과 대응하는 엔티티입니다.
 * <p>
 * <b>주의 — embedding 컬럼 제외</b><br>
 * {@code embedding vector(768)} 컬럼은 JPA가 PostgreSQL {@code vector} 타입을 지원하지 않아
 * 이 엔티티에 매핑하지 않습니다. 실제 벡터 값은 {@code RecipeEmbeddingRepository.upsertEmbedding()}
 * 네이티브 쿼리({@code CAST(? AS vector)})로 직접 삽입·갱신합니다.
 * Hibernate {@code ddl-auto: validate}는 엔티티에 없는 여분 컬럼을 무시하므로 스키마 검증에는 영향 없습니다.
 */
@Entity
@Table(name = "recipe_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeEmbedding {

    /** 자동 증가 기본키. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 레시피 ID({@code recipes.id} 참조 의도).
     * recipes 테이블에 공식 PK 제약이 없어 FK 없이 UNIQUE만 설정.
     */
    @Column(name = "recipe_id", unique = true, nullable = false)
    private Long recipeId;

    /** 행이 처음 생성된 시각(자동). */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 임베딩이 마지막으로 갱신된 시각(upsert 시 native query에서 NOW()로 직접 설정). */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RecipeEmbedding(Long recipeId) {
        this.recipeId = recipeId;
    }
}

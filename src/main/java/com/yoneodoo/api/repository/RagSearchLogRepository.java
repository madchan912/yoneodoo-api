package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.RagSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code rag_search_log} 테이블 접근 계층.
 * <p>
 * {@code RecipeSearchService.search()}에서 식단 조합 완료 후 INSERT 전용으로 사용합니다.
 */
public interface RagSearchLogRepository extends JpaRepository<RagSearchLog, Long> {
}

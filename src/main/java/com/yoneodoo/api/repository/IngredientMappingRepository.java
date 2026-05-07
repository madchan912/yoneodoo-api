package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.IngredientMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link IngredientMapping} 테이블({@code ingredient_mapping})에 대한 DB 접근 계층입니다.
 * <p>
 * 원본 재료명({@code raw_name})이 유니크이므로, "이미 매핑이 있는지" 확인할 때
 * {@link #findByRawName(String)} 한 번으로 조회할 수 있습니다.
 */
public interface IngredientMappingRepository extends JpaRepository<IngredientMapping, Long> {

    /**
     * 정규화된 raw 키로 매핑 한 건을 조회합니다.
     * 없으면 {@link Optional#empty()} — 신규 INSERT vs 기존 UPDATE 분기에 사용됩니다.
     */
    Optional<IngredientMapping> findByRawName(String rawName);

    /**
     * 매핑 전체를 "등록 시각 최신순"으로 가져옵니다.
     * 어드민 "매핑 완료 목록"에서 최근에 정리한 항목이 위로 오도록 할 때 사용합니다.
     */
    List<IngredientMapping> findAllByOrderByCreatedAtDesc();
}

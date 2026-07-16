package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.IngredientNutrition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@code ingredient_nutrition} 테이블 접근 계층입니다.
 * <p>
 * 어드민 영양성분 관리에서 "수동 입력 필요 목록" 조회(source='manual_needed')와
 * 마스터명 기반 단건 조회·업데이트에 사용됩니다.
 */
public interface IngredientNutritionRepository extends JpaRepository<IngredientNutrition, Long> {

    /**
     * 특정 source 값으로 전체 조회(이름 오름차순).
     * "manual_needed" 목록 또는 "foodsafety_kr" 완료 목록 조회에 사용합니다.
     */
    List<IngredientNutrition> findBySourceOrderByMasterNameAsc(String source);

    /**
     * 마스터 재료명으로 단건 조회.
     * ingredient_mapping.master_name과 동일 키로 검색합니다.
     */
    Optional<IngredientNutrition> findByMasterName(String masterName);

    /**
     * 특정 source를 제외한 전체 목록을 이름 오름차순으로 가져옵니다.
     * 완료 탭("source != 'manual_needed'") 조회에 사용합니다.
     */
    List<IngredientNutrition> findBySourceNotOrderByMasterNameAsc(String source);

    /**
     * 특정 source 값의 행 수. 통계(매칭 완료/미매칭 카운트)에 사용합니다.
     */
    long countBySource(String source);
}

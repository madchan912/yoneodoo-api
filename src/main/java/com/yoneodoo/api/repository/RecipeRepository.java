package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.DisplayStatus;
import com.yoneodoo.api.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@link Recipe} 엔티티에 대한 DB 접근 계층입니다.
 * <p>
 * Spring Data JPA가 메서드 이름·{@code @Query}를 보고 SQL을 자동 생성합니다.
 * "서비스/컨트롤러"는 이 인터페이스를 호출해 SELECT·INSERT·UPDATE·DELETE를 수행합니다.
 * <p>
 * <b>표시 상태(Soft Delete) 가드</b><br>
 * 사용자용 조회는 {@link #findByDisplayStatus(DisplayStatus)} 등 {@code displayStatus} 필터가 들어간 메서드를 사용해
 * {@code HIDDEN} 레시피가 노출되지 않게 합니다. 어드민 목록은 전체({@link #findAll()})를 그대로 사용해도 됩니다.
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** {@code status} 컬럼 값이 정확히 일치하는 레시피 행 개수(대시보드 집계용). */
    long countByStatus(String status);

    /** 특정 유튜버명의 레시피 수 집계(유튜버 관리 화면 totalRecipes 표시용). */
    long countByYoutuberName(String youtuberName);

    /** 특정 상태의 레시피 목록 전체(어드민 필터 등). */
    List<Recipe> findByStatus(String status);

    /** 사용자 노출 여부 기준 조회. 사용자용 GET·검색 캐시는 {@code ACTIVE}만 사용. */
    List<Recipe> findByDisplayStatus(DisplayStatus displayStatus);

    /**
     * 사용자 노출 "이중 안전장치" 메서드.
     * <p>
     * 어드민 토글({@code displayStatus = ACTIVE})뿐 아니라 크롤링 파이프라인 결과({@code status = SUCCESS})도 함께 만족해야
     * 사용자 화면(검색·목록)에 나가도록 보장합니다. 자막 추출 실패 등으로 잘못 적재된 행이 새 나가는 것을 막습니다.
     */
    List<Recipe> findByStatusAndDisplayStatus(String status, DisplayStatus displayStatus);

    /**
     * 아직 "완료"나 "자막 없음" 등으로 확정되지 않은 레시피 건수.
     * <p>
     * 기획 관점: 크롤링·자막 파이프라인에서 처리 대기·실패·미분류에 가까운 상태를 하나의 숫자로 보고 싶을 때 사용합니다.
     */
    @Query("""
            SELECT COUNT(r) FROM Recipe r
            WHERE r.status IS NULL
               OR (r.status <> 'SUCCESS' AND r.status <> 'NO_SUBTITLES' AND r.status <> 'SKIP')
            """)
    long countPendingOrUnknown();

    /**
     * 어드민 화면의 "대기/이상 상태" 목록용: 위와 동일한 조건으로 레시피 행을 가져옵니다.
     * 생성일 최신순으로 정렬해 최근 이슈부터 볼 수 있게 합니다.
     */
    @Query("""
            SELECT r FROM Recipe r
            WHERE r.status IS NULL
               OR (r.status <> 'SUCCESS' AND r.status <> 'NO_SUBTITLES' AND r.status <> 'SKIP')
            ORDER BY r.createdAt DESC
            """)
    List<Recipe> findPendingForAdmin();

    /**
     * 특정 재료명(rawName)이 {@code ingredients} JSONB 배열 안에 있는 레시피 목록을 반환합니다.
     * <p>
     * 기획 관점: 어드민이 미분류 재료를 정규화하기 전에 "이 재료가 실제로 어느 레시피에 쓰이는지"
     * 확인하는 용도입니다. {@code jsonb_array_elements}로 배열을 풀어 {@code name} 키 값과 정확 일치 비교합니다.
     * <p>
     * 주의: rawName 은 호출 전에 {@link com.yoneodoo.api.admin.IngredientNameNormalizer}로 정규화해야
     * DB에 저장된 키와 일치합니다.
     *
     * @param rawName 정규화된 재료명 (공백 제거 등 적용된 상태)
     * @return 해당 재료가 포함된 레시피 목록 (생성일 최신순)
     */
    @Query(value = """
            SELECT r.* FROM recipes r
            WHERE r.ingredients IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(r.ingredients) AS elem
                WHERE elem->>'name' = :rawName
              )
            ORDER BY r.created_at DESC
            """, nativeQuery = true)
    List<Recipe> findByIngredientRawName(@Param("rawName") String rawName);

    /**
     * 요리명 키워드로 레시피를 검색합니다 (사용자용 요리명 검색 API 전용).
     * <p>
     * 대소문자를 구분하지 않는 부분 일치(ILIKE)로 {@code title}을 검색합니다.
     * 사용자 노출 이중 안전장치({@code status=SUCCESS}, {@code displayStatus=ACTIVE})를 함께 적용해
     * 정상 처리된 레시피만 반환합니다.
     *
     * @param keyword 사용자가 입력한 요리명 검색어 (빈 문자열이면 호출하지 않는 것이 원칙)
     * @return 제목에 키워드가 포함된 레시피 목록 (생성일 최신순)
     */
    @Query("""
            SELECT r FROM Recipe r
            WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND r.status = 'SUCCESS'
              AND r.displayStatus = 'ACTIVE'
            ORDER BY r.createdAt DESC
            """)
    List<Recipe> searchByTitle(@Param("keyword") String keyword);
}

package com.yoneodoo.api.admin.dto;

/**
 * 어드민 대시보드 상단에 표시할 "숫자 요약" 묶음입니다.
 * <p>
 * 각 필드 의미:<br>
 * {@code totalRecipes} — DB에 쌓인 레시피 총개수<br>
 * {@code successRecipes} — 상태가 SUCCESS로 끝난 레시피 수<br>
 * {@code noSubtitlesRecipes} — 자막이 없어 NO_SUBTITLES로 분류된 수<br>
 * {@code pendingRecipes} — 아직 끝나지 않았거나 상태가 애매한 수(집계 쿼리 기준)<br>
 * {@code unclassifiedIngredients} — 레시피 JSON에는 있는데 매핑 테이블에는 없는 "서로 다른 재료 키" 개수
 */
public record AdminDashboardStatsResponse(
        long totalRecipes,
        long successRecipes,
        long noSubtitlesRecipes,
        long pendingRecipes,
        long unclassifiedIngredients
) {
}

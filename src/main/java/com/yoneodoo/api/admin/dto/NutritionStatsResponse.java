package com.yoneodoo.api.admin.dto;

/**
 * 영양성분 적재 통계 응답 DTO.
 * GET /api/v1/admin/nutrition/stats 에서 반환합니다.
 * 어드민 페이지 상단 요약 카드에 표시됩니다.
 *
 * @param total     ingredient_nutrition 전체 행 수
 * @param matched   source != 'manual_needed'인 행 수(적재 완료)
 * @param unmatched source = 'manual_needed'인 행 수(수동 입력 필요)
 */
public record NutritionStatsResponse(long total, long matched, long unmatched) {
}

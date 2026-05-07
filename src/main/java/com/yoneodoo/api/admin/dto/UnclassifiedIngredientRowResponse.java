package com.yoneodoo.api.admin.dto;

/**
 * "미분류" 재료 한 줄입니다.
 * <p>
 * 의미: 모든 레시피 JSON을 훑어 집계한 재료 키 중, 아직 매핑 테이블에 없는 것만 보여 줍니다.<br>
 * {@code occurrenceCount}: 그 이름이 레시피들에 총 몇 번 등장했는지(우선순위 판단용).
 */
public record UnclassifiedIngredientRowResponse(
        String rawName,
        long occurrenceCount
) {
}

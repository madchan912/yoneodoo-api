package com.yoneodoo.api.admin.dto;

import com.yoneodoo.api.entity.DisplayStatus;

/**
 * 특정 미분류 재료명이 포함된 레시피 한 줄 응답입니다.
 * <p>
 * 사용 화면: 어드민 재료 정규화 페이지에서 특정 raw 재료가 어떤 레시피에 쓰였는지 확인할 때 표시됩니다.
 * 재료 JSON·자막 등 무거운 필드는 제외하고, 식별·분류에 필요한 최소 정보만 포함합니다.
 *
 * @param id            레시피 PK
 * @param title         요리 제목
 * @param youtuberName  크리에이터명
 * @param videoId       유튜브 영상 ID
 * @param status        파이프라인 처리 상태 코드(SUCCESS / NO_SUBTITLES 등)
 * @param displayStatus 사용자 화면 노출 여부(ACTIVE / HIDDEN)
 */
public record UnclassifiedIngredientRecipeResponse(
        Long id,
        String title,
        String youtuberName,
        String videoId,
        String status,
        DisplayStatus displayStatus
) {
}

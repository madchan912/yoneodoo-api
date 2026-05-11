package com.yoneodoo.api.admin.dto;

import com.yoneodoo.api.dto.RecipeIngredientData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 "레시피 상세/수정" 화면용 응답입니다.
 * <p>
 * 목록용 {@link AdminRecipeRowResponse}는 가볍게 만든 반면, 이 DTO는 편집에 필요한 모든 필드
 * (요리명·유튜브 URL·재료 배열 등)를 함께 내려 줍니다.
 *
 * @param id            레시피 PK
 * @param title         요리명
 * @param status        파이프라인 상태 코드(SUCCESS/NO_SUBTITLES 등)
 * @param videoId       유튜브 영상 ID(현재 단계에서는 수정 불가, 표시용)
 * @param youtubeUrl    유튜브 영상 전체 URL
 * @param youtuberName  크리에이터 표시명
 * @param ingredients   재료 배열(이름·분량)
 * @param transcript    자막 원문(표시용, 수정 화면 기본에서는 안 보이게 가능)
 * @param createdAt     최초 적재 시각
 */
public record AdminRecipeDetailResponse(
        Long id,
        String title,
        String status,
        String videoId,
        String youtubeUrl,
        String youtuberName,
        List<RecipeIngredientData> ingredients,
        String transcript,
        LocalDateTime createdAt
) {
}

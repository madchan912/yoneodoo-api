package com.yoneodoo.api.dto;

import com.yoneodoo.api.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 API({@code GET /api/v1/recipes})의 레시피 응답 DTO입니다.
 * <p>
 * {@link Recipe} 엔티티를 직접 노출하지 않고, 클라이언트에 필요한 필드만 담습니다.<br>
 * 제외 필드: {@code status}, {@code displayStatus}(파이프라인 내부 상태),
 * {@code transcript}(대용량 원문 — 목록 API에서 불필요).
 * <p>
 * 재료명은 {@code ingredient_mapping}의 {@code master_name}으로 이미 변환된 상태로 담깁니다.
 */
@Getter
@AllArgsConstructor
public class RecipeResponse {

    /** 레시피 고유 ID. */
    private final Long id;
    /** 요리 제목. */
    private final String title;
    /** 유튜버/채널명. */
    private final String youtuberName;
    /** 유튜브 원본 URL. */
    private final String youtubeUrl;
    /** 유튜브 영상 ID(썸네일·임베드 용도). */
    private final String videoId;
    /** 재료 목록(name은 master_name으로 변환됨). */
    private final List<RecipeIngredientData> ingredients;
    /** DB 적재 시각. */
    private final LocalDateTime createdAt;
    /** 마지막 수정 시각(레시피 수정 시 자동 갱신). */
    private final LocalDateTime updatedAt;

    /**
     * {@link Recipe} 엔티티와 이미 변환된 재료 목록을 받아 DTO를 만듭니다.
     * <p>
     * 재료 master_name 변환은 컨트롤러에서 선행하고, 결과를 이 메서드에 넘깁니다.
     */
    public static RecipeResponse from(Recipe recipe, List<RecipeIngredientData> ingredients) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getYoutuberName(),
                recipe.getYoutubeUrl(),
                recipe.getVideoId(),
                ingredients,
                recipe.getCreatedAt(),
                recipe.getUpdatedAt()
        );
    }
}

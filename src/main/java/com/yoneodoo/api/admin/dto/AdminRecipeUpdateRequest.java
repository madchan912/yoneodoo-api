package com.yoneodoo.api.admin.dto;

import com.yoneodoo.api.dto.RecipeIngredientData;
import com.yoneodoo.api.entity.DisplayStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 어드민이 레시피 한 건을 수정할 때 보내는 요청 본문입니다.
 * <p>
 * 수정 대상 필드:<br>
 * · {@code title} — 요리명(필수, 빈 문자열 금지)<br>
 * · {@code youtubeUrl} — 유튜브 영상 전체 URL (현재 UI에서는 읽기 전용이지만 API는 받음)<br>
 * · {@code ingredients} — 재료 배열. 각 원소는 이름·분량을 갖는 {@link RecipeIngredientData}.<br>
 * · {@code displayStatus} — 사용자 노출 상태(ACTIVE/HIDDEN). null이면 변경 없음.
 * <p>
 * 서버에서는 적재 시와 동일하게 각 재료 이름의 공백을 제거해
 * 검색 캐시·매핑 테이블 키와 규칙을 맞춥니다.
 */
@Data
public class AdminRecipeUpdateRequest {

    /** 요리명. */
    private String title;

    /** 유튜브 영상 전체 URL. */
    private String youtubeUrl;

    /** 재료 배열(이름·분량). */
    private List<RecipeIngredientData> ingredients = new ArrayList<>();

    /** 사용자 노출 상태(ACTIVE/HIDDEN). null이면 기존 값 유지. */
    private DisplayStatus displayStatus;
}

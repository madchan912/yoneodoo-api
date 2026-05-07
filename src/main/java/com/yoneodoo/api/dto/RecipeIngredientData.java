package com.yoneodoo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 레시피 JSON 배열 안에 들어가는 "재료 한 줄"을 표현합니다.
 * <p>
 * DB에서는 {@link com.yoneodoo.api.entity.Recipe#ingredients} jsonb 리스트의 원소로 저장됩니다.
 * <p>
 * <b>기획 시 주의</b><br>
 * {@code name}은 적재 시 공백이 제거되어 저장되는 경우가 있으므로,
 * 매핑 테이블({@code ingredient_mapping})의 키와 같은 규칙으로 맞추는 것이 중요합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientData {
    /** 재료 이름(예: "고추장"). 검색·매핑의 핵심 키로 쓰입니다. */
    private String name;
    /** 분량 텍스트(예: "0.5스푼"). 표시용으로 두고, 현재 검색 키에는 덜 쓰입니다. */
    private String amount;
}

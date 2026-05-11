package com.yoneodoo.api.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * "AI 매핑 추천" 요청 본문입니다.
 * <p>
 * 어드민이 미분류 목록에서 체크한 원본 재료명({@code rawNames})을 Gemini 에 던져,
 * 가장 적절한 "마스터 재료명 한 단어"를 받아오기 위한 입력 묶음입니다.
 *
 * @see IngredientSuggestionResponse
 */
@Data
public class IngredientSuggestionRequest {

    /** 추천을 받을 원본 재료명 목록. 비어 있으면 400. */
    private List<String> rawNames = new ArrayList<>();
}

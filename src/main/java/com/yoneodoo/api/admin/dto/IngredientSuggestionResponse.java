package com.yoneodoo.api.admin.dto;

import java.util.List;

/**
 * "AI 매핑 추천" 응답입니다.
 * <p>
 * 프런트엔드는 {@link #suggestion()} 값을 마스터명 입력창에 자동으로 채워 넣고,
 * 어드민이 직접 확인 후 [매핑 저장] 버튼을 눌러 저장을 확정합니다. (사람-AI 협업; Human-in-the-Loop)
 *
 * @param suggestion  Gemini 가 추천한 마스터 재료명(한 단어, 공백 제거됨)
 * @param rawNames    어떤 원본 재료들을 묶었는지 다시 보내주는 에코(프런트 UI 표시용)
 * @param model       응답에 사용된 모델 ID(예: gemini-1.5-flash) — 감사/디버깅용
 */
public record IngredientSuggestionResponse(
        String suggestion,
        List<String> rawNames,
        String model
) {
}

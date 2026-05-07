package com.yoneodoo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 외부(크롤러 등)가 "새 레시피 저장" API로 보내는 JSON 형태를 담는 전송 객체(DTO)입니다.
 * <p>
 * <b>엔티티({@link com.yoneodoo.api.entity.Recipe})와의 관계</b><br>
 * 필드 구성이 비슷하지만, DTO는 "HTTP 요청 전용"이고 엔티티는 "DB 한 줄"입니다.
 * {@link com.yoneodoo.api.service.RecipeService}가 DTO를 읽어 엔티티로 옮긴 뒤 저장합니다.
 * <p>
 * {@code ingredients}는 DB jsonb에 그대로 들어갈 재료 배열입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCreateRequest {
    /** 유튜브 영상 식별자(중복 방지·링크 구성에 사용). */
    private String videoId;
    /** 요리 제목. */
    private String title;
    /** 영상 전체 URL. */
    private String youtubeUrl;
    /** 크롤링/자막 처리 상태 문자열. */
    private String status;
    /** 자막·스크립트 원문. */
    private String transcript;
    /** 크리에이터 표시명. */
    private String youtuberName;

    /**
     * 재료 배열. 원소 타입은 {@link RecipeIngredientData}.
     * 저장 전 서비스에서 각 {@code name}의 공백을 제거합니다.
     */
    private List<RecipeIngredientData> ingredients;
}

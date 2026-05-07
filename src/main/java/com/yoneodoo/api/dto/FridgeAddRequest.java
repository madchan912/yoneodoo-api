package com.yoneodoo.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 냉장고에 재료를 추가할 때 쓰는 요청 본문 DTO입니다.
 * <p>
 * {@code userId}: 어느 회원의 냉장고를 수정할지(현재는 클라이언트가 숫자로 보냄 — 추후 로그인 토큰으로 대체 가능).<br>
 * {@code ingredients}: 추가할 재료 "이름" 문자열들의 목록(예: {@code ["계란","고추장"]}).
 */
@Getter
@NoArgsConstructor
public class FridgeAddRequest {
    /** 대상 회원의 PK. */
    private Long userId;

    /** 한 번에 여러 재료를 넣을 수 있는 이름 배열. */
    private List<String> ingredients;
}

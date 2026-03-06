package com.yoneodoo.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FridgeAddRequest {
    // 임시로 유저 ID를 받습니다. (나중에 로그인 기능이 생기면 토큰에서 빼서 쓸 예정!)
    private Long userId;

    // 추가할 재료들의 ID 목록 (예: [1, 2, 4])
    private List<Long> ingredientIds;
}
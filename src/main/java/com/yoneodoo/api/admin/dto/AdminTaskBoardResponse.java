package com.yoneodoo.api.admin.dto;

import java.time.LocalDateTime;

/**
 * 어드민 "로드맵" 화면에 보여 줄 {@code TASK.md} 파일 내용을 전달하는 응답입니다.
 *
 * @param path     실제 파일을 찾은 절대/상대 경로(디버깅·표시용)
 * @param content  마크다운 원문(프런트에서 react-markdown 등으로 렌더)
 * @param readAt   서버가 파일을 읽은 시각(캐시 무효화·갱신 표시용)
 */
public record AdminTaskBoardResponse(
        String path,
        String content,
        LocalDateTime readAt
) {
}

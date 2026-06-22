package com.yoneodoo.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * 전역 예외 처리기입니다. 모든 컨트롤러에서 발생한 예외를 잡아 일관된 JSON 형식으로 응답합니다.
 * <p>
 * 처리 우선순위(위에서 아래로):<br>
 * ① {@link ResponseStatusException} — 서비스/컨트롤러에서 명시적으로 던진 HTTP 상태 예외. 지정 상태코드 그대로 반환.<br>
 * ② {@link IllegalArgumentException} — 입력값 검증 실패. 400 Bad Request.<br>
 * ③ {@link RuntimeException} — 그 외 런타임 예외(예: 유저 없음). 500 Internal Server Error.<br>
 * ④ {@link Exception} — 예상치 못한 모든 예외. 500, 상세 메시지 숨김.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ① 서비스 레이어에서 직접 던진 {@link ResponseStatusException} 처리.
     * 지정된 HTTP 상태와 reason 메시지를 그대로 JSON으로 감쌉니다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(status, e.getReason(), LocalDateTime.now()));
    }

    /**
     * ② 입력값 검증 실패({@link IllegalArgumentException}) → 400 Bad Request.
     * AdminService 등에서 유효하지 않은 파라미터 진입 시 발생합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, e.getMessage(), LocalDateTime.now()));
    }

    /**
     * ③ 그 외 런타임 예외(예: UserFridgeService의 "유저를 찾을 수 없습니다.") → 500.
     * {@link IllegalArgumentException}과 {@link ResponseStatusException}은 위에서 먼저 처리됩니다.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, e.getMessage(), LocalDateTime.now()));
    }

    /**
     * ④ catch-all — 예상치 못한 예외. 내부 메시지를 외부에 노출하지 않습니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "서버 오류가 발생했습니다.", LocalDateTime.now()));
    }

    /**
     * 에러 응답 공통 형식.
     *
     * @param status    HTTP 상태 코드(숫자)
     * @param message   사람이 읽을 수 있는 오류 메시지
     * @param timestamp 오류 발생 시각
     */
    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
}

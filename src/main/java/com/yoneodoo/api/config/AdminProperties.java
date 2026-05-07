package com.yoneodoo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 어드민 API 접근에 쓰는 공유 비밀값 설정입니다.
 * <p>
 * application.yaml 등에서 {@code yoneodoo.admin.secret} 키로 주입됩니다.
 * 이 값과 동일한 문자열을 HTTP 헤더 {@code X-Admin-Secret}에 실어 보낸 요청만 어드민 엔드포인트를 통과합니다.
 * <p>
 * {@code secret}이 비어 있으면 {@link AdminSecretAuthFilter}가 어드민 경로를 막아,
 * 설정 누락 상태로 운영 DB가 노출되는 일을 줄입니다.
 */
@ConfigurationProperties(prefix = "yoneodoo.admin")
public record AdminProperties(String secret) {

    /** 시크릿이 null/공백이 아닌지 — 필터에서 서비스 가능 여부 판단에 사용. */
    public boolean hasSecret() {
        return secret != null && !secret.isBlank();
    }
}

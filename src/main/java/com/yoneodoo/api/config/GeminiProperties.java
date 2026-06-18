package com.yoneodoo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Gemini API 호출 설정입니다.
 * <p>
 * application.yaml 에서 {@code gemini.*} 키로 주입됩니다.
 * <ul>
 *   <li>{@code gemini.api-key} — Google AI Studio 에서 발급한 API 키 (로컬 시크릿 파일에서만 관리, Git 커밋 금지)</li>
 *   <li>{@code gemini.model} — 호출할 모델 ID. 기본값 {@code gemini-2.5-flash}</li>
 *   <li>{@code gemini.base-url} — Generative Language API 베이스 URL. 기본값은 공식 엔드포인트.</li>
 *   <li>{@code gemini.connect-timeout-ms} — 커넥션 수립 한도(ms). 기본 30초.</li>
 *   <li>{@code gemini.read-timeout-ms} — LLM 응답 대기 한도(ms). 기본 180초(=3분). 청크가 크면 모델 생성 시간이 길어져 넉넉히 둡니다.</li>
 *   <li>{@code gemini.timeout-ms} — (legacy) 단일 값. 위 두 필드가 비어 있으면 폴백으로 사용.</li>
 * </ul>
 * <p>
 * 키가 비어 있으면 호출 서비스에서 503/사용 불가로 응답하도록 합니다.
 * 운영(prod)에서는 환경변수 {@code GEMINI_API_KEY} 로 주입하는 것을 권장합니다.
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        Integer timeoutMs,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {
    public GeminiProperties {
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        }
        if (connectTimeoutMs == null || connectTimeoutMs <= 0) {
            // 커넥션 수립 단계는 비교적 짧음. 기본 30초.
            connectTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 30_000;
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            // LLM 추론은 입력이 크면 1~2분이 정상. 기본 3분(=180초).
            readTimeoutMs = (timeoutMs != null && timeoutMs > 0) ? Math.max(timeoutMs, 180_000) : 180_000;
        }
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = readTimeoutMs;
        }
    }

    /** API 키가 실제로 들어와 있는지 — 키가 없으면 추천 기능을 503으로 막기 위함. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

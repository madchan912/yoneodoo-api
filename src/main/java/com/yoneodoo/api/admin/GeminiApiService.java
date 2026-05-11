package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.yoneodoo.api.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Google Generative Language API(Gemini)로의 공통 HTTP 호출입니다.
 * <p>
 * 모델 엔드포인트는 {@code gemini-1.5-flash-latest} 고정 URL을 사용합니다.
 * 예전처럼 {@code /models/gemini-1.5-flash} 만 조합하면 404 가 나는 환경이 있어,
 * 공식 문서 형태의 전체 경로를 한 곳에서만 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiApiService {

    /**
     * generateContent 전체 URL의 경로 부분(쿼리 {@code key} 제외).
     * 실제 요청: {@code GENERATE_CONTENT_PATH}?key={apiKey}
     */
    public static final String GENERATE_CONTENT_PATH =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";

    private final GeminiProperties props;
    private final RestClient geminiRestClient;

    /**
     * Gemini {@code generateContent} 한 번 호출하고 원본 JSON 응답 트리를 돌려줍니다.
     *
     * @param body 요청 본문({@code contents}, {@code generationConfig} 등)
     * @throws ResponseStatusException 키 미설정(503), Gemini 오류(502), 타임아웃(504)
     */
    public JsonNode generateContent(Map<String, Object> body) {
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }
        String uri = GENERATE_CONTENT_PATH + "?key=" + props.apiKey();
        try {
            return geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini HTTP {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini call failed: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.warn("Gemini timeout/network error", e);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini call timed out");
        }
    }
}

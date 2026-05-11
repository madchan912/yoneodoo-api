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

import java.net.URI;
import java.util.Map;

/**
 * Google Generative Language API(Gemini)로의 공통 HTTP 호출입니다.
 * <p>
 * 모델 엔드포인트는 {@code gemini-2.5-flash} 전체 URL을 한 곳에서만 관리합니다.
 * <p>
 * <b>주의 — URL 인코딩</b><br>
 * Gemini 경로의 {@code :generateContent} 콜론은 RFC상 path 에서 허용되지만, Spring 의
 * URL 템플릿(String 오버로드)을 거치면 {@code %3A} 로 인코딩되어 404 가 납니다.
 * 그래서 호출 시 {@link URI#create(String)} 로 미리 {@link URI} 객체를 만들어 그대로 넘깁니다.
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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

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
        String url = GENERATE_CONTENT_PATH + "?key=" + props.apiKey();
        // 콜론(:) 자동 인코딩 → 404 방지를 위해 URI 객체로 감싸 그대로 전달
        URI uri = URI.create(url);
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

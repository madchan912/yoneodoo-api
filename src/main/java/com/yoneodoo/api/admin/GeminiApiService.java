package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
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
 * <p>
 * <b>주의 — JsonNode 역직렬화</b><br>
 * {@code RestClient.retrieve().body(JsonNode.class)} 는 내부적으로
 * {@code objectMapper.readValue(inputStream, JsonNode.class)} 를 호출하는데,
 * {@code JsonNode} 가 추상 클래스라 Jackson 의 일반 타입 해석 경로에서 실패합니다.
 * 대신 응답을 {@code String} 으로 받은 뒤 {@code objectMapper.readTree()} 로 파싱합니다.
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gemini {@code text-embedding-004} 모델로 텍스트를 768차원 벡터로 변환합니다.
     * <p>
     * ① 텍스트를 {@code embedContent} 엔드포인트에 POST<br>
     * ② 응답 {@code embedding.values} 배열을 {@code List<Double>} 로 반환<br>
     * <p>
     * 호출 전에 {@link #isConfigured()} 가 true 여야 합니다.
     *
     * @param text 임베딩할 텍스트 (레시피명 + 재료 목록 형태 권장)
     * @return 768차원 부동소수점 벡터
     * @throws ResponseStatusException 키 미설정(503), 빈 응답(502), API 오류(502/504)
     */
    public List<Double> embedContent(String text) {
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent"
                + "?key=" + props.apiKey();
        URI uri = URI.create(url);

        Map<String, Object> body = Map.of(
                "model", "models/text-embedding-004",
                "content", Map.of("parts", List.of(Map.of("text", text)))
        );

        try {
            String responseBody = geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini embedding returned empty response");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode values = root.path("embedding").path("values");
            if (values.isMissingNode() || !values.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Unexpected Gemini embedding response: " + responseBody.substring(0, Math.min(200, responseBody.length())));
            }

            List<Double> vector = new ArrayList<>(values.size());
            for (JsonNode v : values) {
                vector.add(v.asDouble());
            }
            return vector;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini embed HTTP {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini embed failed: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.warn("Gemini embed timeout/network error", e);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini embed call timed out");
        } catch (Exception e) {
            log.warn("Failed to parse Gemini embedding response", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Gemini embedding response");
        }
    }

    /**
     * Gemini {@code generateContent} 한 번 호출하고 원본 JSON 응답 트리를 돌려줍니다.
     * <p>
     * ① 응답을 {@code String} 으로 수신 — {@code StringHttpMessageConverter} 가 처리하므로 타입 오류 없음.<br>
     * ② {@code objectMapper.readTree(responseBody)} 로 파싱 — Jackson 의 추상 타입 처리 경로를 올바르게 사용.<br>
     *
     * @param body 요청 본문({@code contents}, {@code generationConfig} 등)
     * @throws ResponseStatusException 키 미설정(503), 빈 응답(502), Gemini 오류(502), 타임아웃(504)
     */
    public JsonNode generateContent(Map<String, Object> body) {
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }
        String url = GENERATE_CONTENT_PATH + "?key=" + props.apiKey();
        // 콜론(:) 자동 인코딩 → 404 방지를 위해 URI 객체로 감싸 그대로 전달
        URI uri = URI.create(url);
        try {
            // ① String 으로 받아 추상 타입 역직렬화 오류 회피
            String responseBody = geminiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned empty response body");
            }

            // ② readTree() 로 JsonNode 파싱 — JsonNode 추상 클래스를 안전하게 처리하는 Jackson 전용 경로
            return objectMapper.readTree(responseBody);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini HTTP {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini call failed: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.warn("Gemini timeout/network error", e);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gemini call timed out");
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response as JSON", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Gemini response as JSON");
        }
    }
}

package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.dto.IngredientSuggestionResponse;
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

import java.util.List;
import java.util.Map;

/**
 * "AI 매핑 추천" 도메인 서비스 — Google Gemini API 를 호출해 마스터 재료명 한 단어를 받아옵니다.
 * <p>
 * <b>호출 흐름</b><br>
 * ① 어드민이 미분류 목록에서 선택한 {@code rawNames} 를 받아 입력 검증.<br>
 * ② {@link #buildPrompt(List)} 로 한국어 재료 정규화 프롬프트 생성.<br>
 * ③ {@code POST {baseUrl}/models/{model}:generateContent?key={apiKey}} 로 Gemini 호출.<br>
 * ④ 응답 본문에서 {@code candidates[0].content.parts[0].text} 를 꺼내 JSON 으로 파싱.<br>
 * ⑤ {@code masterName} 한 단어를 추출해 공백 제거 후 반환.
 * <p>
 * <b>사람 검수 정책 (Human-in-the-Loop)</b><br>
 * 본 서비스는 결과를 DB에 저장하지 않습니다. 프런트는 추천값을 입력창에 채워 주기만 하고,
 * 최종 저장은 사람이 직접 [매핑 저장] 버튼을 눌러 확정합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientSuggestionService {

    private final GeminiProperties props;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gemini 호출 진입점. 추천 마스터 재료명을 만들어 돌려줍니다.
     *
     * @param rawNames 어드민이 선택한 원본 재료명 목록 (1개 이상)
     * @return 추천값을 담은 응답 DTO
     * @throws ResponseStatusException 입력이 비었거나(400) Gemini 키 미설정(503) 또는 외부 호출 실패(502)
     */
    public IngredientSuggestionResponse suggest(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rawNames must not be empty");
        }
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }

        List<String> cleaned = rawNames.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .limit(40) // 한 번에 너무 많이 보내지 않게 상한
                .toList();
        if (cleaned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rawNames must contain at least one non-blank value");
        }

        String prompt = buildPrompt(cleaned);
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        String path = "/models/" + props.model() + ":generateContent?key=" + props.apiKey();
        JsonNode root;
        try {
            root = geminiRestClient.post()
                    .uri(path)
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

        String suggestion = extractMasterName(root);
        if (suggestion == null || suggestion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned an empty suggestion");
        }
        return new IngredientSuggestionResponse(suggestion, cleaned, props.model());
    }

    /**
     * Gemini 에 보낼 한국어 프롬프트.
     * 응답을 안전하게 파싱하기 위해 단일 JSON 객체 {@code {"masterName":"..."}} 형식만 받도록 명시합니다.
     */
    private String buildPrompt(List<String> rawNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국어 요리 재료 정규화 어시스턴트입니다.\n");
        sb.append("아래 재료명 목록을 보고, 이들을 모두 대표할 수 있는 \"마스터 재료명\" 단 한 단어를 추천하세요.\n\n");
        sb.append("규칙:\n");
        sb.append("1. 가능한 가장 일반적이고 짧은 한 단어 (예: \"대파\",\"쪽파\",\"실파\" → \"파\")\n");
        sb.append("2. 공백·특수문자·괄호·수량·단위 모두 제거된 깔끔한 한 단어\n");
        sb.append("3. 영문/숫자 섞임 없이 한국어로\n");
        sb.append("4. 반드시 JSON 한 줄만 응답: {\"masterName\":\"...\"}\n");
        sb.append("5. 그 외 설명/마크다운 코드블록 금지\n\n");
        sb.append("재료 목록:\n");
        for (String name : rawNames) {
            sb.append("- ").append(name).append("\n");
        }
        return sb.toString();
    }

    /**
     * Gemini 응답에서 마스터명을 꺼냅니다.
     * <p>
     * 응답 구조: {@code candidates[0].content.parts[0].text} 안에 모델이 만든 JSON 문자열이 들어 있습니다.
     * 일부 응답은 코드블록(```json ... ```)으로 감싸기도 하므로 {@code {} } 의 첫·마지막 위치로 잘라
     * 보수적으로 파싱합니다.
     */
    private String extractMasterName(JsonNode root) {
        if (root == null) return null;
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode() || text.isNull()) return null;
        String raw = text.asText("").trim();
        if (raw.isEmpty()) return null;

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        String jsonOnly = raw.substring(start, end + 1);

        try {
            JsonNode parsed = objectMapper.readTree(jsonOnly);
            String name = parsed.path("masterName").asText("");
            return name.replaceAll("\\s+", "");
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response JSON: {}", raw);
            return null;
        }
    }
}

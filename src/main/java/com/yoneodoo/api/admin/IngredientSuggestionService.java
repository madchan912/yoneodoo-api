package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.dto.IngredientSuggestionResponse;
import com.yoneodoo.api.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * "AI 매핑 추천" 도메인 서비스 — Google Gemini API 를 호출해 마스터 재료명 한 단어를 받아옵니다.
 * <p>
 * 실제 HTTP 호출은 {@link GeminiApiService} 가 고정 URL({@code gemini-2.5-flash})로 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientSuggestionService {

    private final GeminiProperties props;
    private final GeminiApiService geminiApiService;
    /** Spring Boot auto-configure 된 ObjectMapper 빈 — GeminiApiService 와 동일한 인스턴스를 공유합니다. */
    private final ObjectMapper objectMapper;

    /**
     * Gemini 호출 진입점. 추천 마스터 재료명을 만들어 돌려줍니다.
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
                .limit(40)
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

        JsonNode root = geminiApiService.generateContent(body);

        String suggestion = extractMasterName(root);
        if (suggestion == null || suggestion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned an empty suggestion");
        }
        return new IngredientSuggestionResponse(suggestion, cleaned, "gemini-2.5-flash");
    }

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

package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 미분류 재료 전체를 Gemini 에게 넘겨 "마스터 재료 기준 그룹" JSON 을 받아옵니다.
 * <p>
 * DB에는 저장하지 않습니다. 관리자가 프런트 승인 모달에서 검토한 뒤 {@code bulk-map} 으로 확정합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientBulkGroupingService {

    private final AdminService adminService;
    private final GeminiApiService geminiApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 현재 미분류인 모든 {@code raw_name} 을 조회해 Gemini 로 그룹핑하고,
     * 미분류 집합에 속하는 항목만 남긴 {@code Map&lt;마스터, raw 목록&gt;} 을 반환합니다.
     */
    public Map<String, List<String>> suggestBulkGroupingForAllUnclassified() {
        List<UnclassifiedIngredientRowResponse> rows = adminService.listUnclassifiedIngredients();
        Set<String> allowed = rows.stream()
                .map(UnclassifiedIngredientRowResponse::rawName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (allowed.isEmpty()) {
            return Map.of();
        }

        List<String> ordered = new ArrayList<>(allowed);
        String prompt = buildBulkGroupingPrompt(ordered);

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
        return parseAndFilterGrouping(root, allowed);
    }

    private String buildBulkGroupingPrompt(List<String> rawNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국어 요리 재료 정규화 전문가입니다.\n");
        sb.append("주어진 재료명 배열을 분석해서, 같은 마스터 재료(기본 식재료명)를 공유하는 것들끼리 묶어줘.\n\n");
        sb.append("응답 규칙:\n");
        sb.append("- 반드시 JSON 객체 한 개만 반환해. 키는 마스터 재료명(짧은 한국어 단어), 값은 그 마스터에 속하는 원본 재료명 문자열의 배열이야.\n");
        sb.append("- 예: {\"양파\":[\"다진 양파\",\"양파 반개\"],\"대파\":[\"대파 1뿌리\",\"쪽파\"]}\n");
        sb.append("- 마크다운 백틱(```json)이나 코드펜스는 절대 쓰지 마. 설명 문장도 붙이지 마.\n");
        sb.append("- 입력에 없는 재료명을 만들어내지 마.\n\n");
        sb.append("재료명 배열:\n");
        sb.append(objectMapper.valueToTree(rawNames).toString());
        return sb.toString();
    }

    /**
     * Gemini 응답에서 JSON 객체를 파싱하고, 허용된 미분류 키만 남기며 중복 raw 는 첫 등장 그룹만 유지합니다.
     */
    private Map<String, List<String>> parseAndFilterGrouping(JsonNode root, Set<String> allowed) {
        String rawText = extractResponseText(root);
        if (rawText == null || rawText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned empty body");
        }

        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("No JSON object in Gemini response: {}", rawText);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini response is not a JSON object");
        }

        JsonNode obj;
        try {
            obj = objectMapper.readTree(rawText.substring(start, end + 1));
        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON: {}", rawText, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse Gemini JSON");
        }

        if (!obj.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini JSON root must be an object");
        }

        Set<String> seenRaw = new LinkedHashSet<>();
        Map<String, List<String>> out = new LinkedHashMap<>();

        java.util.Iterator<String> fieldNames = obj.fieldNames();
        while (fieldNames.hasNext()) {
            String masterKey = fieldNames.next();
            String masterNorm = IngredientNameNormalizer.normalize(masterKey);
            if (masterNorm.isEmpty()) {
                continue;
            }
            JsonNode arr = obj.get(masterKey);
            if (arr == null || !arr.isArray()) {
                continue;
            }
            List<String> bucket = new ArrayList<>();
            for (JsonNode n : arr) {
                if (!n.isTextual()) {
                    continue;
                }
                String rawNorm = IngredientNameNormalizer.normalize(n.asText());
                if (rawNorm.isEmpty() || !allowed.contains(rawNorm) || seenRaw.contains(rawNorm)) {
                    continue;
                }
                seenRaw.add(rawNorm);
                bucket.add(rawNorm);
            }
            if (!bucket.isEmpty()) {
                out.put(masterNorm, bucket);
            }
        }

        return out;
    }

    private String extractResponseText(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!text.isMissingNode() && text.isTextual()) {
            return text.asText();
        }
        return null;
    }
}

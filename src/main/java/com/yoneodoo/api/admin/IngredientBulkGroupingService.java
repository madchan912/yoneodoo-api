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
 * <p>
 * <b>청크 호출</b><br>
 * 미분류가 많으면 한 번에 보내기엔 입력이 커서 LLM 추론 시간이 길어지고 {@code Read timed out} 위험이 큽니다.
 * 그래서 {@link #CHUNK_SIZE} (=50) 개씩 나누어 여러 번 호출한 뒤, 결과 {@code Map&lt;master, raw[]&gt;} 들을 병합합니다.
 * 청크 간 같은 {@code raw} 가 두 마스터에 들어오면 <b>먼저 등장한 청크</b>의 그룹만 유지합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientBulkGroupingService {

    /** 한 번의 Gemini 호출에 보낼 최대 raw 개수. 추론 시간과 길이 제약을 고려한 안전치. */
    static final int CHUNK_SIZE = 50;

    private final AdminService adminService;
    private final GeminiApiService geminiApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 현재 미분류인 모든 {@code raw_name} 을 50건 단위 청크로 Gemini 에 보내 그룹핑한 뒤,
     * 모든 청크 결과를 하나의 {@code Map&lt;마스터, raw 목록&gt;} 으로 병합해 돌려줍니다.
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
        List<List<String>> chunks = partition(ordered, CHUNK_SIZE);
        log.info("Gemini bulk-suggest: total={}, chunkSize={}, chunks={}", ordered.size(), CHUNK_SIZE, chunks.size());

        Set<String> seenRaw = new LinkedHashSet<>();
        Map<String, List<String>> merged = new LinkedHashMap<>();

        for (int i = 0; i < chunks.size(); i++) {
            List<String> chunk = chunks.get(i);
            log.info("Gemini bulk-suggest chunk {}/{} size={}", i + 1, chunks.size(), chunk.size());

            String prompt = buildBulkGroupingPrompt(chunk);
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
            Map<String, List<String>> partial = parseAndFilterGrouping(root, allowed, seenRaw);
            mergeInto(merged, partial);
        }

        return merged;
    }

    /**
     * 입력 리스트를 {@code size} 개씩 잘라 부분 리스트들의 리스트로 만듭니다.
     * 외부 의존성 없이 Java Stream/서브리스트로 구현합니다.
     */
    private static <T> List<List<T>> partition(List<T> source, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            out.add(new ArrayList<>(source.subList(i, Math.min(i + size, source.size()))));
        }
        return out;
    }

    /**
     * 청크별 부분 결과를 누적 맵에 합칩니다. 같은 마스터 키가 또 오면 raw 목록을 이어 붙입니다.
     */
    private static void mergeInto(Map<String, List<String>> merged, Map<String, List<String>> partial) {
        partial.forEach((master, raws) ->
                merged.computeIfAbsent(master, k -> new ArrayList<>()).addAll(raws));
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
     *
     * @param seenRaw 호출 측이 누적해 주는 "이미 어떤 청크에서 채택된 raw" 집합. 청크 간 중복도 차단됩니다.
     */
    private Map<String, List<String>> parseAndFilterGrouping(JsonNode root, Set<String> allowed, Set<String> seenRaw) {
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

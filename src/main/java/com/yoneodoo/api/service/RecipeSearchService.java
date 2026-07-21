package com.yoneodoo.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.GeminiApiService;
import com.yoneodoo.api.dto.MealPlanResponse;
import com.yoneodoo.api.entity.RagSearchLog;
import com.yoneodoo.api.repository.RagSearchLogRepository;
import com.yoneodoo.api.repository.RecipeEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 기반 식단 플래너 서비스입니다.
 * <p>
 * <b>파이프라인 흐름</b><br>
 * ① 사용자 자연어 → Gemini generateContent로 식단 조건 JSON 추출<br>
 * ② 추출된 조건 텍스트 → Gemini embedContent로 768차원 벡터화<br>
 * ③ pgvector 코사인 유사도 검색 → 후보 레시피 최대 20건<br>
 * ④ 후보 레시피 + 조건 → Gemini generateContent로 N일 식단 텍스트 생성<br>
 * <p>
 * exclude_ingredients 필터는 pgvector 검색 후 Java에서 적용합니다.
 * (재료 정보가 JSONB로 저장되어 native query 조인 복잡도 회피)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeSearchService {

    private final GeminiApiService geminiApiService;
    private final RecipeEmbeddingRepository embeddingRepository;
    private final RagSearchLogRepository ragSearchLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자 쿼리를 받아 RAG 식단 플랜을 생성합니다.
     * <p>
     * ① 조건 추출 → ② 벡터화 → ③ 유사도 검색 → ④ 식단 조합
     *
     * @param userQuery 사용자 자연어 입력
     * @return 식단 텍스트, 후보 레시피 목록, 추출된 조건
     */
    public MealPlanResponse search(String userQuery) {
        // ① 조건 추출
        Map<String, Object> conditions = extractConditions(userQuery);
        log.info("조건 추출 완료: {}", conditions);

        Double maxCalories = conditions.get("max_calories") instanceof Number n
                ? n.doubleValue() : null;
        @SuppressWarnings("unchecked")
        List<String> excludeIngredients = conditions.get("exclude_ingredients") instanceof List<?> l
                ? (List<String>) l : List.of();
        String goal = (String) conditions.getOrDefault("goal", null);
        int days = conditions.get("days") instanceof Number n ? n.intValue() : 7;

        // ② 벡터화 — 조건 텍스트를 임베딩
        String conditionText = buildConditionText(conditions);
        List<Double> vector = geminiApiService.embedContent(conditionText);
        String vectorJson = toVectorJson(vector);

        // ③ pgvector 유사도 검색
        List<Object[]> rows = embeddingRepository.findSimilarRecipes(vectorJson, maxCalories);

        // exclude_ingredients 필터 (Java 레이어)
        List<Map<String, Object>> candidates = rows.stream()
                .map(this::rowToMap)
                .filter(r -> excludeIngredients.isEmpty()
                        || excludeIngredients.stream().noneMatch(ex ->
                        r.getOrDefault("title", "").toString().contains(ex)))
                .collect(Collectors.toList());

        log.info("후보 레시피 {}건 (제외 후)", candidates.size());

        // ④ 식단 조합
        String mealPlan = generateMealPlan(candidates, conditions, days, goal, maxCalories, excludeIngredients);

        saveSearchLog(userQuery, conditions, candidates, mealPlan);

        return new MealPlanResponse(mealPlan, candidates, conditions);
    }

    /**
     * 식단 조합 완료 후 사용 이력을 {@code rag_search_log}에 저장합니다.
     * userId는 소셜 로그인 도입 전까지 null로 저장합니다. 저장 실패는 검색 결과에 영향 주지 않습니다.
     */
    private void saveSearchLog(String userQuery, Map<String, Object> conditions,
                                List<Map<String, Object>> candidates, String mealPlan) {
        try {
            String conditionsJson = objectMapper.writeValueAsString(conditions);
            String recipesJson = objectMapper.writeValueAsString(candidates);
            ragSearchLogRepository.save(RagSearchLog.of(null, userQuery, conditionsJson, recipesJson, mealPlan));
        } catch (Exception e) {
            log.warn("RAG 검색 로그 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * ① Gemini로 사용자 입력에서 식단 조건 JSON을 추출합니다.
     * 파싱 실패 시 기본값(days=7, 나머지 null/빈 값)을 반환합니다.
     */
    private Map<String, Object> extractConditions(String userInput) {
        String prompt = """
                아래 사용자 입력에서 식단 조건을 JSON으로 추출해줘.
                입력: %s
                출력 형식:
                {
                  "max_calories": 숫자 또는 null,
                  "exclude_ingredients": ["재료1", "재료2"] 또는 [],
                  "goal": "diet" | "muscle" | "balanced" | null,
                  "days": 숫자 (기본 7)
                }
                JSON만 반환. 다른 텍스트 금지.
                """.formatted(userInput);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        try {
            JsonNode root = geminiApiService.generateContent(body);
            String json = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            // 마크다운 코드블록 제거
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            JsonNode node = objectMapper.readTree(json);
            Map<String, Object> result = new HashMap<>();
            if (!node.path("max_calories").isNull()) result.put("max_calories", node.path("max_calories").asDouble());
            else result.put("max_calories", null);
            List<String> excludes = new ArrayList<>();
            node.path("exclude_ingredients").forEach(n -> excludes.add(n.asText()));
            result.put("exclude_ingredients", excludes);
            result.put("goal", node.path("goal").isNull() ? null : node.path("goal").asText());
            result.put("days", node.path("days").asInt(7));
            return result;
        } catch (Exception e) {
            log.warn("조건 추출 실패, 기본값 사용: {}", e.getMessage());
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("max_calories", null);
            defaults.put("exclude_ingredients", List.of());
            defaults.put("goal", null);
            defaults.put("days", 7);
            return defaults;
        }
    }

    /**
     * ② 조건 맵을 임베딩용 텍스트로 변환합니다.
     * 예: "식단 목표: diet, 칼로리 제한: 500kcal, 제외 재료: 두부, 계란"
     */
    private String buildConditionText(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder("식단 조건: ");
        if (conditions.get("goal") != null) sb.append("목표=").append(conditions.get("goal")).append(" ");
        if (conditions.get("max_calories") != null) sb.append("칼로리=").append(conditions.get("max_calories")).append("kcal이하 ");
        @SuppressWarnings("unchecked")
        List<String> excludes = (List<String>) conditions.getOrDefault("exclude_ingredients", List.of());
        if (!excludes.isEmpty()) sb.append("제외재료=").append(String.join(",", excludes));
        return sb.toString().trim();
    }

    /** DB native query 결과 Object[]를 Map으로 변환합니다. */
    private Map<String, Object> rowToMap(Object[] row) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", row[0]);
        map.put("title", row[1]);
        map.put("video_id", row[2]);
        map.put("youtuber_name", row[3]);
        map.put("calories", row[4]);
        map.put("protein", row[5]);
        map.put("coverage_pct", row[6]);
        map.put("similarity", row[7]);
        return map;
    }

    /**
     * ④ 후보 레시피와 조건을 Gemini에 전달해 N일 식단 텍스트를 생성합니다.
     * 후보가 없으면 안내 문구를 반환합니다.
     */
    private String generateMealPlan(List<Map<String, Object>> recipes, Map<String, Object> conditions,
                                    int days, String goal, Double maxCalories, List<String> excludeIngredients) {
        if (recipes.isEmpty()) {
            return "조건에 맞는 레시피를 찾을 수 없습니다. 조건을 조정해 다시 시도해 주세요.";
        }

        String recipeList = recipes.stream()
                .map(r -> "- %s (%.0fkcal)".formatted(
                        r.getOrDefault("title", ""),
                        r.get("calories") instanceof Number n ? n.doubleValue() : 0.0))
                .collect(Collectors.joining("\n"));

        String prompt = """
                아래 레시피 목록으로 %d일 식단을 짜줘.
                목표: %s
                칼로리 제한: %s
                제외 재료: %s

                레시피 목록:
                %s

                출력 형식:
                1일차: 레시피명 (NNNkcal)
                2일차: 레시피명 (NNNkcal)
                ...

                없는 레시피는 만들지 말고 목록에서만 선택할 것.
                """.formatted(
                days,
                goal != null ? goal : "제한 없음",
                maxCalories != null ? maxCalories + "kcal 이하" : "제한 없음",
                excludeIngredients.isEmpty() ? "없음" : String.join(", ", excludeIngredients),
                recipeList
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            JsonNode root = geminiApiService.generateContent(body);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("식단 생성에 실패했습니다.");
        } catch (Exception e) {
            log.warn("식단 생성 실패: {}", e.getMessage());
            return "식단 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String toVectorJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("벡터 직렬화 실패", e);
        }
    }
}

package com.yoneodoo.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.GeminiApiService;
import com.yoneodoo.api.dto.RecipeIngredientData;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 레시피 임베딩 생성·저장 서비스입니다.
 * <p>
 * <b>파이프라인 위치</b><br>
 * 레시피가 저장된 직후({@code RecipeService.saveRecipe}) 호출되어,
 * Gemini {@code text-embedding-004}로 768차원 벡터를 만들고
 * {@code recipe_embeddings} 테이블에 upsert합니다.
 * <p>
 * 임베딩 실패는 레시피 저장 트랜잭션에 영향을 주지 않도록 호출부에서 try-catch로 감쌉니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeEmbeddingService {

    private final GeminiApiService geminiApiService;
    private final RecipeEmbeddingRepository embeddingRepository;
    private final ObjectMapper objectMapper;

    /**
     * 레시피 한 건을 임베딩해 {@code recipe_embeddings}에 upsert합니다.
     * <p>
     * ① 레시피명 + 재료 목록을 한 문자열로 조합<br>
     * ② Gemini embedContent API 호출 → 768차원 List&lt;Double&gt;<br>
     * ③ JSON 배열 문자열로 변환 → native upsert ({@code ::vector} 캐스트)<br>
     *
     * @param recipe 임베딩할 레시피 (id, title, ingredients 필드 필요)
     */
    @Transactional
    public void embedAndSave(Recipe recipe) {
        String text = buildEmbeddingText(recipe);
        log.info("임베딩 생성 시작 — recipe_id={} text_len={}", recipe.getId(), text.length());

        List<Double> vector = geminiApiService.embedContent(text);

        String vectorJson = toVectorJson(vector);
        embeddingRepository.upsertEmbedding(recipe.getId(), vectorJson);

        log.info("임베딩 저장 완료 — recipe_id={} dims={}", recipe.getId(), vector.size());
    }

    /**
     * 레시피명과 재료 목록을 임베딩용 텍스트로 조합합니다.
     * 예: "레시피명: 닭가슴살볶음\n재료: 닭가슴살, 브로콜리, 간장"
     */
    private String buildEmbeddingText(Recipe recipe) {
        String ingredients = "";
        if (recipe.getIngredients() != null) {
            ingredients = recipe.getIngredients().stream()
                    .map(RecipeIngredientData::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining(", "));
        }
        return "레시피명: " + recipe.getTitle() + "\n재료: " + ingredients;
    }

    /** List&lt;Double&gt;을 pgvector가 인식하는 "[0.1, 0.2, ...]" 형태로 직렬화합니다. */
    private String toVectorJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("벡터 직렬화 실패", e);
        }
    }
}

package com.yoneodoo.api.admin;

import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.service.RecipeEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 레시피 임베딩 관련 어드민 API입니다.
 * <p>
 * {@code /api/v1/admin/**} 경로는 {@code AdminSecretAuthFilter}가 X-Admin-Secret 헤더를 검사합니다.
 * 실제 임베딩 생성 로직은 {@link RecipeEmbeddingService}에 위임합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/embeddings")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingAdminController {

    private final RecipeEmbeddingService embeddingService;
    private final RecipeRepository recipeRepository;

    /**
     * {@code recipe_embeddings}에 없는 레시피를 순차적으로 임베딩해 적재합니다.
     * <p>
     * ① recipe_embeddings에 아직 없는 레시피 조회 (limit 지정 시 해당 건수만)<br>
     * ② 각 레시피에 대해 Gemini embedContent 호출 → upsert (실패 시 건너뜀)<br>
     * ③ API 쿼터 보호를 위해 호출 간 4초 딜레이, 429 시 30초 대기 후 최대 3회 재시도<br>
     *
     * @param limit 처리할 최대 건수 (생략 시 전체)
     * @return {@code { "total": N, "success": N, "failed": N }}
     */
    @PostMapping("/backfill")
    public Map<String, Integer> backfill(@RequestParam(required = false) Integer limit) {
        List<Recipe> all = recipeRepository.findRecipesWithoutEmbeddings();
        List<Recipe> targets = (limit != null && limit > 0 && limit < all.size()) ? all.subList(0, limit) : all;
        log.info("임베딩 백필 시작 — 전체 미처리 {}건, 이번 대상 {}건", all.size(), targets.size());

        int success = 0;
        int failed = 0;

        for (Recipe recipe : targets) {
            boolean saved = false;
            for (int attempt = 1; attempt <= 3 && !saved; attempt++) {
                try {
                    embeddingService.embedAndSave(recipe);
                    saved = true;
                    success++;
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("백필 인터럽트 — 중단");
                    return Map.of("total", targets.size(), "success", success, "failed", failed);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("429") && attempt < 3) {
                        log.warn("429 쿼터 초과 — recipe_id={} {}회차, 30초 대기", recipe.getId(), attempt);
                        try { Thread.sleep(30000); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return Map.of("total", targets.size(), "success", success, "failed", failed);
                        }
                    } else {
                        log.warn("임베딩 실패 — recipe_id={} msg={}", recipe.getId(), msg);
                        failed++;
                    }
                }
            }
        }

        log.info("임베딩 백필 완료 — total={} success={} failed={}", targets.size(), success, failed);
        return Map.of("total", targets.size(), "success", success, "failed", failed);
    }
}

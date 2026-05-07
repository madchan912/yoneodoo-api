package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.RecipeCreateRequest;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레시피 "적재(저장)" 비즈니스 로직을 담당하는 서비스입니다.
 * <p>
 * <b>전체 흐름(기획자용 한 줄 요약)</b><br>
 * 외부 크롤러가 보낸 JSON({@link RecipeCreateRequest})을 받아 → 재료 이름을 시스템 규칙에 맞게 정리하고
 * → {@link Recipe} 엔티티로 옮긴 뒤 → {@link RecipeRepository#save(Object)}로 DB {@code recipes} 테이블에 한 행을 넣습니다.
 * <p>
 * <b>왜 재료 이름에서 공백을 제거하는가</b><br>
 * 이후 재료 검색 캐시({@link IngredientSearchService})·재료 매핑({@code ingredient_mapping})과
 * <b>같은 문자열 규칙</b>을 맞추기 위함입니다. 공백 유무로 같은 재료가 다른 것으로 쪼개지면 통계·검색 품질이 떨어집니다.
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    /** 레시피 행을 실제 DB에 INSERT/UPDATE 하는 저장소(인터페이스). */
    private final RecipeRepository recipeRepository;

    /**
     * 크롤러 등 외부 시스템이 보낸 레시피 한 건을 DB에 저장합니다.
     * <p>
     * 처리 단계:<br>
     * 1) 요청 본문의 재료 목록이 있으면, 각 재료의 {@code name}에서 공백을 제거(레시피 JSON 내부 규칙 통일).<br>
     * 2) 제목·URL·유튜버명으로 {@link Recipe} 객체를 만들고, 영상 ID·상태·자막·재료 리스트를 setter로 채웁니다.<br>
     * 3) {@code recipeRepository.save} 호출 시 JPA가 INSERT를 실행하고, jsonb 컬럼에 재료 배열을 직렬화합니다.
     *
     * @param request 저장할 레시피 데이터 묶음(DTO). DB 엔티티가 아니라 "요청 전용 형태"입니다.
     */
    @Transactional
    public void saveRecipe(RecipeCreateRequest request) {
        // ① 재료명 클렌징: 모든 공백 문자를 제거해 이후 검색·매핑 키와 일치시킵니다.
        if (request.getIngredients() != null) {
            request.getIngredients().forEach(ing -> {
                if (ing.getName() != null) {
                    ing.setName(ing.getName().replace(" ", ""));
                }
            });
        }

        // ② 엔티티 생성: 생성자로 필수 텍스트 필드를 채우고, 나머지는 아래에서 세팅합니다.
        Recipe recipe = new Recipe(
                request.getTitle(),
                request.getYoutubeUrl(),
                request.getYoutuberName()
        );

        recipe.setVideoId(request.getVideoId());
        recipe.setStatus(request.getStatus());
        recipe.setTranscript(request.getTranscript());

        // ③ JSONB 매핑: List<RecipeIngredientData>를 그대로 엔티티에 넣으면 Hibernate가 jsonb로 변환합니다.
        recipe.setIngredients(request.getIngredients());

        // ④ 영속화: 트랜잭션 커밋 시점에 DB에 반영됩니다.
        recipeRepository.save(recipe);
    }
}

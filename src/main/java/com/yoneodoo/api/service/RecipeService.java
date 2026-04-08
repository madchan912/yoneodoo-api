package com.yoneodoo.api.service;

import com.yoneodoo.api.dto.RecipeCreateRequest;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    @Transactional
    public void saveRecipe(RecipeCreateRequest request) {
        // 1. 데이터 클렌징: 재료 이름에서 모든 띄어쓰기를 강제로 제거합니다.
        if (request.getIngredients() != null) {
            request.getIngredients().forEach(ing -> {
                if (ing.getName() != null) {
                    ing.setName(ing.getName().replace(" ", ""));
                }
            });
        }

        // 2. 새 레시피 엔티티 생성 및 데이터 세팅
        // (Recipe 엔티티의 생성자가 protected이므로 기본 생성자 호출 후 Setter 사용)
        Recipe recipe = new Recipe(request.getTitle(), request.getYoutubeUrl());

        recipe.setVideoId(request.getVideoId());
        recipe.setStatus(request.getStatus());
        recipe.setTranscript(request.getTranscript());

        // 별도의 변환 작업 없이 리스트를 그대로 세팅 (Hibernate가 JSONB로 자동 처리)
        recipe.setIngredients(request.getIngredients());

        // 3. DB 저장
        recipeRepository.save(recipe);
    }
}
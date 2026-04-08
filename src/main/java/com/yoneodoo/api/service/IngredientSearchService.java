package com.yoneodoo.api.service;

import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository; // 🚀 기존 IngredientRepository에서 변경!
import com.yoneodoo.api.util.KoreanParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    // 🚀 진짜 데이터가 있는 레시피 저장소를 불러옵니다.
    private final RecipeRepository recipeRepository;

    // RAM에 들고 있을 캐시 리스트
    private final List<IngredientCacheDto> ingredientCache = new ArrayList<>();

    // 서버가 켜질 때 실행 (이제 DB에서 진짜 재료를 가져옵니다!)
    @PostConstruct
    public void initCache() {
        // 1. DB에서 모든 레시피 가져오기
        List<Recipe> allRecipes = recipeRepository.findAll();

        // 2. 레시피 안의 JSONB 재료 데이터에서 '이름'만 쏙쏙 뽑아 중복 제거(Set)
        Set<String> uniqueNames = allRecipes.stream()
                .filter(recipe -> recipe.getIngredients() != null) // 재료가 있는 레시피만
                .flatMap(recipe -> recipe.getIngredients().stream()) // 리스트 안의 리스트 평탄화
                .map(ingredient -> ingredient.getName()) // 이름(name)만 추출
                .filter(name -> name != null && !name.trim().isEmpty()) // 빈 값 제거
                .map(name -> name.replace(" ", "")) // 🚀 한 번 더 띄어쓰기 완벽 파괴!
                .collect(Collectors.toSet());

        // 3. 기존 캐시 초기화
        ingredientCache.clear();

        // 4. 추출된 진짜 재료들로 검색용 캐시 생성
        long idCounter = 1L;
        for (String name : uniqueNames) {
            String chosung = KoreanParserUtil.getChosung(name);
            String jamo = KoreanParserUtil.getJamo(name);

            ingredientCache.add(new IngredientCacheDto(idCounter++, name, chosung, jamo));
        }
        System.out.println("✅ 요리 재료 인메모리 캐싱 완료! (진짜 데이터 총 " + ingredientCache.size() + "개)");
    }

    // 🚀 검색 로직은 이미 완벽하게 짜여 있으므로 그대로 유지!
    public List<IngredientCacheDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        boolean isOnlyChosung = keyword.matches("^[ㄱ-ㅎ]+$");

        return ingredientCache.stream()
                .filter(dto -> {
                    if (isOnlyChosung) {
                        return dto.getChosung().contains(keyword);
                    } else {
                        String searchJamo = KoreanParserUtil.getJamo(keyword);
                        return dto.getJamo().contains(searchJamo);
                    }
                })
                .sorted((a, b) -> {
                    // 검색어로 시작하는 단어를 최우선으로 올림
                    boolean aStarts = isOnlyChosung ? a.getChosung().startsWith(keyword) : a.getName().startsWith(keyword);
                    boolean bStarts = isOnlyChosung ? b.getChosung().startsWith(keyword) : b.getName().startsWith(keyword);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return a.getName().compareTo(b.getName());
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public static class IngredientCacheDto {
        private final Long id;
        private final String name;
        private final String chosung;
        private final String jamo;
    }
}
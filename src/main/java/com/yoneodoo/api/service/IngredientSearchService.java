package com.yoneodoo.api.service;

import com.yoneodoo.api.entity.Ingredient;
import com.yoneodoo.api.repository.IngredientRepository;
import com.yoneodoo.api.util.KoreanParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    private final IngredientRepository ingredientRepository;

    // RAM에 들고 있을 캐시 리스트
    private final List<IngredientCacheDto> ingredientCache = new ArrayList<>();

    // 서버가 켜질 때 DB에서 모든 재료를 가져와 메모리에 캐싱 (딱 한 번 실행)
    @PostConstruct
    public void initCache() {
        List<Ingredient> allIngredients = ingredientRepository.findAll();

        for (Ingredient ingredient : allIngredients) {
            String name = ingredient.getName();
            String chosung = KoreanParserUtil.getChosung(name); // 예: "ㅈㄱㅈ"
            String jamo = KoreanParserUtil.getJamo(name);       // 예: "ㅈㅣㄴㄱㅏㄴㅈㅏㅇ"

            ingredientCache.add(new IngredientCacheDto(ingredient.getId(), name, chosung, jamo));
        }
        System.out.println("✅ 요리 재료 인메모리 캐싱 완료! (총 " + ingredientCache.size() + "개)");
    }

    // 프론트엔드에서 검색어가 들어왔을 때 실행되는 핵심 메서드
    public List<IngredientCacheDto> search(String keyword) {
        // 1. 검색어가 없거나 공백이면 빈 리스트 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 검색어가 순수 자음(초성)으로만 이루어져 있는지 확인 (예: "ㄱㅈ")
        boolean isOnlyChosung = keyword.matches("^[ㄱ-ㅎ]+$");

        // 검색어 상태에 따라 캐시된 데이터와 비교
        return ingredientCache.stream()
                .filter(dto -> {
                    if (isOnlyChosung) {
                        // 초성 검색인 경우 ("ㄱㅈ" -> 캐시된 초성 "ㅈㄱㅈ"에서 검색)
                        return dto.getChosung().contains(keyword);
                    } else {
                        // 일반 검색인 경우 ("가" -> "ㄱㅏ"로 분리 후 캐시된 자모에서 검색)
                        String searchJamo = KoreanParserUtil.getJamo(keyword);
                        return dto.getJamo().contains(searchJamo);
                    }
                })
                // 🔥 예전에 아쉬우셨던 부분 완벽 해결: 이름 기준 오름차순(가나다순) 정렬!
                .sorted(Comparator.comparing(IngredientCacheDto::getName))
                // 화면 렌더링 성능을 위해 상위 20개까지만 잘라서 반환
                .limit(20)
                .collect(Collectors.toList());
    }

    // 내부에서 사용할 심플한 DTO (이 데이터를 프론트엔드로 바로 내려줍니다)
    @Getter
    @RequiredArgsConstructor
    public static class IngredientCacheDto {
        private final Long id;
        private final String name;
        private final String chosung;
        private final String jamo;
    }
}
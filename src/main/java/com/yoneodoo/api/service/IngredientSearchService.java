package com.yoneodoo.api.service;

import com.yoneodoo.api.repository.IngredientRepository;
import com.yoneodoo.api.util.KoreanParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    private final IngredientRepository ingredientRepository;

    // RAM에 들고 있을 캐시 리스트
    private final List<IngredientCacheDto> ingredientCache = new ArrayList<>();

    // 서버가 켜질 때 실행 (DB가 비어있으니 더미 데이터를 강제 주입!)
    @PostConstruct
    public void initCache() {
        List<String> dummyNames = Arrays.asList(
                "계란", "간장", "올리고당", "청양고추", "닭다리살", "다진마늘", "설탕",
                "감자", "스팸", "고추장", "고춧가루", "닭가슴살", "양배추", "쌈장", "마늘",
                "소고기", "미역", "국간장", "참기름", "고등어"
        );

        long idCounter = 1L;
        for (String name : dummyNames) {
            String chosung = KoreanParserUtil.getChosung(name); // 예: "ㅈㄱㅈ"
            String jamo = KoreanParserUtil.getJamo(name);       // 예: "ㅈㅣㄴㄱㅏㄴㅈㅏㅇ"

            ingredientCache.add(new IngredientCacheDto(idCounter++, name, chosung, jamo));
        }
        System.out.println("✅ 요리 재료 인메모리 캐싱 완료! (더미 데이터 총 " + ingredientCache.size() + "개)");
    }

    // 검색어가 들어왔을 때 실행되는 핵심 메서드
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
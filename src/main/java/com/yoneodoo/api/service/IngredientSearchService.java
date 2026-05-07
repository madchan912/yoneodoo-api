package com.yoneodoo.api.service;

import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.util.KoreanParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "재료 자동완성/검색"을 위한 서비스입니다.
 * <p>
 * <b>핵심 아이디어(DB vs 메모리)</b><br>
 * 매 검색마다 모든 레시피의 jsonb 재료를 훑으면 DB 부하가 큽니다.
 * 그래서 서버 기동 시점과 매핑 변경 직후 등에 <b>한 번에 모아서 메모리 리스트({@link #ingredientCache})</b>에 올려 두고,
 * 실제 검색은 이 메모리에서 빠르게 필터링합니다.
 * <p>
 * <b>데이터 원천</b><br>
 * {@link RecipeRepository#findAll()}로 모든 레시피를 읽은 뒤, 각 레시피의 {@code ingredients} JSON 배열에서
 * {@code name}만 모아 중복을 제거한 집합을 만듭니다. 즉 "DB에 있는 모든 레시피"가 캐시의 소스입니다.
 * <p>
 * <b>한글 검색 보조</b><br>
 * {@link KoreanParserUtil}로 초성·자모 문자열을 미리 만들어 두고,
 * 사용자가 초성만 입력했는지·완성형을 쳤는지에 따라 비교 방식을 나눕니다.
 */
@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    /** 레시피 전체를 읽어와 캐시를 재구성할 때 사용하는 저장소. */
    private final RecipeRepository recipeRepository;

    /**
     * DB를 직접 두드리지 않고 검색에 사용하는 인메모리 목록입니다.
     * 원소 타입 {@link IngredientCacheDto}에는 표시용 이름과 초성/자모 필드가 함께 들어 있습니다.
     */
    private final List<IngredientCacheDto> ingredientCache = new ArrayList<>();

    /**
     * 스프링 빈이 처음 만들어진 직후(서버 기동 직후) 자동으로 한 번 실행됩니다.
     * <p>
     * 하는 일: DB의 모든 레시피 → 재료 이름 수집·정규화 → 캐시 리스트 재작성.
     * <p>
     * 또한 어드민에서 재료 매핑을 저장/삭제한 뒤에도 {@link #initCache()}를 다시 호출해
     * "DB와 캐시의 재료 목록"이 어긋나지 않게 맞춥니다.
     */
    @PostConstruct
    public void initCache() {
        // ① DB에서 레시피 전체 로드(트래픽이 큰 작업이므로 자주 호출하면 안 되고, 배치성으로 호출하는 설계).
        List<Recipe> allRecipes = recipeRepository.findAll();

        // ② 각 레시피의 JSON 재료에서 name만 뽑아, 공백 제거 후 중복 없는 집합(Set)으로 만듭니다.
        Set<String> uniqueNames = allRecipes.stream()
                .filter(recipe -> recipe.getIngredients() != null)
                .flatMap(recipe -> recipe.getIngredients().stream())
                .map(ingredient -> ingredient.getName())
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(name -> name.replace(" ", ""))
                .collect(Collectors.toSet());

        // ③ 이전 캐시 비우기(완전 재빌드).
        ingredientCache.clear();

        // ④ 집합의 각 재료명에 대해 초성/자모를 계산해 DTO로 캐시에 적재합니다.
        long idCounter = 1L;
        for (String name : uniqueNames) {
            String chosung = KoreanParserUtil.getChosung(name);
            String jamo = KoreanParserUtil.getJamo(name);

            ingredientCache.add(new IngredientCacheDto(idCounter++, name, chosung, jamo));
        }
        System.out.println("✅ 요리 재료 인메모리 캐싱 완료! (진짜 데이터 총 " + ingredientCache.size() + "개)");
    }

    /**
     * 사용자가 입력한 키워드로 재료 후보를 검색합니다.
     * <p>
     * 동작 요약:<br>
     * · 키워드가 비어 있으면 빈 목록.<br>
     * · 키워드가 "ㄱㄴㄷ"처럼 초성만이면 {@code chosung} 필드에 부분 문자열 포함 여부로 필터.<br>
     * · 그 외에는 키워드를 자모로 풀어 {@code jamo} 필드에 부분 문자열 포함 여부로 필터.<br>
     * · "검색어로 시작하는 항목"을 앞쪽으로 정렬한 뒤 최대 20건만 잘라 반환합니다.
     *
     * @param keyword 사용자 입력 문자열(앱의 검색창 값)
     * @return 자동완성 후보 목록(최대 20개)
     */
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
                    boolean aStarts = isOnlyChosung ? a.getChosung().startsWith(keyword) : a.getName().startsWith(keyword);
                    boolean bStarts = isOnlyChosung ? b.getChosung().startsWith(keyword) : b.getName().startsWith(keyword);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return a.getName().compareTo(b.getName());
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * 메모리 캐시에 들어가는 "재료 한 줄" 표현입니다.
     * <p>
     * {@code id}: 캐시 내부용 임시 식별자(DB PK와 무관).<br>
     * {@code name}: 사용자에게 보여 줄 재료명(공백 제거된 형태).<br>
     * {@code chosung}: 초성만 이어 붙인 문자열(초성 검색용).<br>
     * {@code jamo}: 완전 분해한 자모열(완성형 검색용).
     */
    @Getter
    @RequiredArgsConstructor
    public static class IngredientCacheDto {
        private final Long id;
        private final String name;
        private final String chosung;
        private final String jamo;
    }
}

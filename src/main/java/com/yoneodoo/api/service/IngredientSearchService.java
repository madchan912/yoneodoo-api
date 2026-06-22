package com.yoneodoo.api.service;

import com.yoneodoo.api.entity.IngredientMapping;
import com.yoneodoo.api.repository.IngredientMappingRepository;
import com.yoneodoo.api.util.KoreanParserUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 재료 자동완성·검색과 raw→master 이름 변환을 담당하는 서비스입니다.
 * <p>
 * <b>캐시 데이터 원천</b><br>
 * {@code ingredient_mapping} 테이블의 {@code master_name}만 추출해 중복 제거 후 인메모리 캐시에 올립니다.
 * 따라서 검색 자동완성은 마스터(표준) 재료명 기준으로 동작합니다.
 * <p>
 * <b>raw→master 변환</b><br>
 * {@link #toMaster(String)}으로 레시피 JSON의 raw 재료명을 master_name으로 변환할 수 있습니다.
 * 매핑이 없는 raw_name은 원본 그대로 반환합니다.
 * <p>
 * <b>한글 검색 보조</b><br>
 * {@link KoreanParserUtil}로 초성·자모 문자열을 미리 만들어 두고,
 * 사용자가 초성만 입력했는지·완성형을 쳤는지에 따라 비교 방식을 나눕니다.
 */
@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    /** ingredient_mapping 테이블에 접근해 master_name 목록과 raw→master 맵을 구성합니다. */
    private final IngredientMappingRepository ingredientMappingRepository;

    /**
     * DB를 직접 두드리지 않고 검색에 사용하는 인메모리 목록입니다.
     * 원소 타입 {@link IngredientCacheDto}에는 표시용 master_name과 초성/자모 필드가 함께 들어 있습니다.
     */
    private final List<IngredientCacheDto> ingredientCache = new ArrayList<>();

    /**
     * raw_name → master_name 변환 맵(인메모리).
     * {@link #toMaster(String)} 호출 시 O(1)로 조회합니다.
     */
    private Map<String, String> rawToMasterMap = new HashMap<>();

    /**
     * 스프링 빈이 처음 만들어진 직후(서버 기동 직후) 자동으로 한 번 실행됩니다.
     * <p>
     * 하는 일:<br>
     * ① {@code ingredient_mapping} 전체 행을 읽어 raw→master 맵을 구성합니다.<br>
     * ② master_name을 중복 제거(Set)한 뒤 초성/자모 인덱스와 함께 캐시에 적재합니다.<br>
     * <p>
     * 어드민에서 재료 매핑을 저장/삭제한 뒤에도 이 메서드를 다시 호출해 캐시를 갱신합니다.
     */
    @PostConstruct
    public void initCache() {
        // ① ingredient_mapping 테이블 전체 로드
        List<IngredientMapping> mappings = ingredientMappingRepository.findAll();

        // ② raw→master 변환 맵 재구성 (raw_name이 유니크이므로 충돌 없음)
        rawToMasterMap = mappings.stream()
                .collect(Collectors.toMap(
                        IngredientMapping::getRawName,
                        IngredientMapping::getMasterName,
                        (a, b) -> a  // 만일을 위한 중복 키 처리(첫 번째 우선)
                ));

        // ③ master_name만 중복 제거해 집합으로 만듭니다.
        Set<String> masterNames = mappings.stream()
                .map(IngredientMapping::getMasterName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        // ④ 이전 캐시 비우기(완전 재빌드).
        ingredientCache.clear();

        // ⑤ 각 master_name에 대해 초성/자모를 계산해 DTO로 캐시에 적재합니다.
        long idCounter = 1L;
        for (String name : masterNames) {
            String chosung = KoreanParserUtil.getChosung(name);
            String jamo = KoreanParserUtil.getJamo(name);
            ingredientCache.add(new IngredientCacheDto(idCounter++, name, chosung, jamo));
        }
        System.out.println("✅ 재료 마스터명 캐시 완료! (master 종류 " + ingredientCache.size() + "개, raw 매핑 " + rawToMasterMap.size() + "건)");
    }

    /**
     * 사용자가 입력한 키워드로 마스터 재료 후보를 검색합니다.
     * <p>
     * 동작 요약:<br>
     * · 키워드가 비어 있으면 빈 목록.<br>
     * · 키워드가 "ㄱㄴㄷ"처럼 초성만이면 {@code chosung} 필드에 부분 문자열 포함 여부로 필터.<br>
     * · 그 외에는 키워드를 자모로 풀어 {@code jamo} 필드에 부분 문자열 포함 여부로 필터.<br>
     * · "검색어로 시작하는 항목"을 앞쪽으로 정렬한 뒤 최대 20건만 잘라 반환합니다.
     *
     * @param keyword 사용자 입력 문자열(앱의 검색창 값)
     * @return 자동완성 후보 목록(최대 20개, master_name 기준)
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
     * raw_name을 master_name으로 변환합니다.
     * <p>
     * {@code ingredient_mapping}에 등록된 raw_name이면 해당 master_name을 반환하고,
     * 등록되지 않은 raw_name은 원본 그대로 반환합니다.
     *
     * @param rawName 레시피 JSON에 저장된 원본 재료명
     * @return 대응하는 master_name, 없으면 rawName 그대로
     */
    public String toMaster(String rawName) {
        if (rawName == null) return null;
        return rawToMasterMap.getOrDefault(rawName, rawName);
    }

    /**
     * 메모리 캐시에 들어가는 "재료 한 줄" 표현입니다.
     * <p>
     * {@code id}: 캐시 내부용 임시 식별자(DB PK와 무관).<br>
     * {@code name}: 사용자에게 보여 줄 master_name.<br>
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

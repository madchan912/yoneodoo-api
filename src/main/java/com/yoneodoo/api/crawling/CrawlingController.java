package com.yoneodoo.api.crawling;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 크롤링 관련 보조 데이터({@link CrawlingData})를 조회하는 HTTP 진입점입니다.
 * <p>
 * 핵심 레시피 본문은 {@link com.yoneodoo.api.controller.RecipeController} 경로로 적재하는 흐름이 더 중심이며,
 * 이 컨트롤러는 {@code crawling_data} 테이블 점검·디버깅용에 가깝습니다.
 */
@RestController
@RequestMapping("/api/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingDataRepository repository;

    /** 크롤링 데이터 테이블의 모든 행을 그대로 반환합니다(페이징 없음 — 데이터 적을 때 위주). */
    @GetMapping
    public List<CrawlingData> getAllData() {
        return repository.findAll();
    }
}

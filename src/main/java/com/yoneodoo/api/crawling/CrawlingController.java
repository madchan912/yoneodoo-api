package com.yoneodoo.api.crawling;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingDataRepository repository;

    @GetMapping
    public List<CrawlingData> getAllData() {
        // 일꾼(Repository)을 시켜서 창고(DB)에 있는 모든 데이터를 꺼내서 반환!
        return repository.findAll();
    }
}
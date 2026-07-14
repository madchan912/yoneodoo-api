package com.yoneodoo.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 어드민이 크롤링을 수동 트리거할 때 보내는 요청 본문입니다.
 * <p>
 * {@code POST /api/v1/admin/crawl} → Spring → {@code POST http://localhost:8000/crawl} (FastAPI) 순으로 전달됩니다.
 * FastAPI가 snake_case를 기대하므로 {@code @JsonProperty}로 직렬화 키를 맞춥니다.
 */
@Data
public class CrawlTriggerRequest {

    /** 크롤링할 유튜브 채널 URL. 예: {@code https://www.youtube.com/@유지만} */
    @JsonProperty("channel_url")
    private String channelUrl;

    /** 채널 숏츠 목록에서 가져올 시작 인덱스 (1-based). */
    private int start = 1;

    /** 채널 숏츠 목록에서 가져올 끝 인덱스 (inclusive). */
    private int end = 10;
}

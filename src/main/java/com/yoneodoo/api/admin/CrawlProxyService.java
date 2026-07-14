package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.CrawlTriggerRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * FastAPI 데이터 파이프라인 서버({@code yoneodoo-data})에 크롤링 요청을 중계하는 서비스입니다.
 * <p>
 * <b>흐름</b><br>
 * 어드민 UI → {@code POST /api/v1/admin/crawl} → 이 서비스 → {@code POST {dataUrl}/crawl} (FastAPI)<br>
 * 어드민 UI → {@code GET  /api/v1/admin/crawl/status/{jobId}} → 이 서비스 → {@code GET {dataUrl}/status/{jobId}}
 * <p>
 * FastAPI 서버 URL은 환경변수 {@code YONEODOO_DATA_URL}로 변경할 수 있으며, 기본값은 {@code http://localhost:8000}입니다.
 * EC2 배포 시 api·data 컨테이너가 같은 호스트에 있으므로 기본값 그대로 사용합니다.
 */
@Service
public class CrawlProxyService {

    private final RestClient restClient;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    public CrawlProxyService(
            @Value("${yoneodoo.data.url:http://localhost:8000}") String dataUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(dataUrl)
                .build();
    }

    /**
     * FastAPI에 채널 크롤링을 요청하고 job_id를 포함한 응답을 그대로 반환합니다.
     * <p>
     * ① 요청 본문을 FastAPI가 기대하는 snake_case JSON으로 직렬화.<br>
     * ② FastAPI 응답 JSON({@code job_id}, {@code status})을 Map으로 역직렬화해 그대로 전달.
     *
     * @param request 채널 URL·범위 정보
     * @return FastAPI 응답 그대로 — 최소한 {@code job_id}, {@code status} 키 포함
     * @throws ResponseStatusException 502 — FastAPI 서버 연결 실패 또는 오류 응답
     */
    public Map<String, Object> triggerCrawl(CrawlTriggerRequest request) {
        try {
            return restClient.post()
                    .uri("/crawl")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "데이터 파이프라인 서버 연결 실패: " + e.getMessage());
        }
    }

    /**
     * FastAPI에서 크롤링 job 진행 상태를 조회해 그대로 반환합니다.
     * <p>
     * FastAPI 응답에는 {@code status}(pending/running/done/failed), {@code processed},
     * {@code total}, {@code results} 등이 포함됩니다.
     *
     * @param jobId {@link #triggerCrawl}이 반환한 job_id
     * @return FastAPI 상태 응답 그대로
     * @throws ResponseStatusException 502 — FastAPI 서버 연결 실패 또는 오류 응답
     */
    public Map<String, Object> getCrawlStatus(String jobId) {
        try {
            return restClient.get()
                    .uri("/status/{jobId}", jobId)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "데이터 파이프라인 서버 연결 실패: " + e.getMessage());
        }
    }
}

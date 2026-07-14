package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.CrawlTriggerRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * FastAPI 데이터 파이프라인 서버({@code yoneodoo-data})에 크롤링 요청을 중계하는 서비스입니다.
 * <p>
 * <b>흐름</b><br>
 * 어드민 UI → {@code POST /api/v1/admin/crawl} → 이 서비스 → {@code POST {dataUrl}/crawl} (FastAPI)<br>
 * 어드민 UI → {@code GET  /api/v1/admin/crawl/status/{jobId}} → 이 서비스 → {@code GET {dataUrl}/status/{jobId}}
 * <p>
 * FastAPI 서버 URL은 환경변수 {@code YONEODOO_DATA_URL}로 변경할 수 있으며, 기본값은 {@code http://localhost:8000}입니다.
 */
@Service
public class CrawlProxyService {

    private final String dataUrl;
    // RestClient.Builder 빈 주입 문제를 피하기 위해 RestTemplate 사용.
    // RestTemplate은 classpath에 jackson-databind가 있으면 자동으로 JSON 직렬화를 지원합니다.
    private final RestTemplate restTemplate = new RestTemplate();

    public CrawlProxyService(
            @Value("${yoneodoo.data.url:http://localhost:8000}") String dataUrl) {
        this.dataUrl = dataUrl;
    }

    /**
     * FastAPI에 채널 크롤링을 요청하고 job_id를 포함한 응답을 그대로 반환합니다.
     * <p>
     * ① FastAPI가 기대하는 snake_case 필드({@code channel_url}, {@code start}, {@code end})만 Map으로 구성.<br>
     * ② {@code Content-Type: application/json} 헤더와 함께 POST 전송.<br>
     * ③ FastAPI 응답 JSON({@code job_id}, {@code status})을 Map으로 역직렬화해 그대로 전달.
     *
     * @param request 채널 URL·범위 정보
     * @return FastAPI 응답 그대로 — 최소한 {@code job_id}, {@code status} 키 포함
     * @throws ResponseStatusException 502 — FastAPI 서버 연결 실패 또는 오류 응답
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> triggerCrawl(CrawlTriggerRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "channel_url", request.getChannelUrl() != null ? request.getChannelUrl() : "",
                    "start", request.getStart(),
                    "end", request.getEnd()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(dataUrl + "/crawl", entity, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "데이터 파이프라인 서버 오류: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "데이터 파이프라인 서버 연결 실패: " + e.getMessage());
        }
    }

    /**
     * FastAPI에서 채널 전체 숏츠 수를 조회합니다.
     * <p>
     * 크롤링 트리거 UI에서 끝 인덱스 기본값 설정에 사용합니다.
     * scrapetube가 전체 영상을 fetch하므로 채널 규모에 따라 응답이 느릴 수 있습니다.
     *
     * @param channelUrl 유튜브 채널 URL
     * @return {@code channel_url}, {@code total_videos} 포함 Map
     * @throws ResponseStatusException 502 — FastAPI 서버 연결 실패
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getChannelInfo(String channelUrl) {
        try {
            String url = UriComponentsBuilder.fromUriString(dataUrl + "/channel-info")
                    .queryParam("channel_url", channelUrl)
                    .build()
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "채널 정보 조회 실패: " + e.getMessage());
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
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCrawlStatus(String jobId) {
        try {
            return restTemplate.getForObject(dataUrl + "/status/" + jobId, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "데이터 파이프라인 서버 연결 실패: " + e.getMessage());
        }
    }
}

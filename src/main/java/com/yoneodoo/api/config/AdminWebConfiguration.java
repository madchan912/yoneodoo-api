package com.yoneodoo.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 어드민·외부 API(Gemini) 관련 설정 빈을 한곳에서 활성화합니다.
 * <p>
 * {@link RestClient}는 Gemini 호출처럼 외부 HTTPS를 부르는 동기 클라이언트입니다.
 * 커넥션 수립과 응답 대기는 서로 다른 단계라 타임아웃을 분리해서 부여합니다.
 * <ul>
 *   <li>connectTimeout — TCP/TLS 핸드셰이크 한도(짧음). 기본 30초.</li>
 *   <li>readTimeout — LLM 추론 결과를 기다리는 시간(길음). 기본 3분(180초). 미분류가 많으면 모델 생성 시간이 길어지므로 넉넉히 둡니다.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({AdminProperties.class, GeminiProperties.class})
public class AdminWebConfiguration {

    /**
     * Gemini 전용 {@link RestClient} 빈.
     * baseUrl 까지 미리 셋업하므로, 서비스에서는 상대 경로(예: {@code /models/{model}:generateContent})만 신경 쓰면 됩니다.
     */
    @Bean
    public RestClient geminiRestClient(GeminiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }
}

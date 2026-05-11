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
 * 타임아웃은 {@link GeminiProperties#timeoutMs()} 기준으로 부여합니다.
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
        factory.setConnectTimeout(props.timeoutMs());
        factory.setReadTimeout(props.timeoutMs());
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }
}

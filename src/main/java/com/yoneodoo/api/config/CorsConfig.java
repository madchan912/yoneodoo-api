package com.yoneodoo.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 정책을 한 곳에서 관리합니다.
 * <p>
 * 각 컨트롤러에 흩어져 있던 {@code @CrossOrigin}을 제거하고,
 * 허용 오리진·메서드를 여기서 일괄 적용합니다.
 * <p>
 * 허용 오리진: 로컬 개발({@code localhost:5173}), 운영 EC2({@code 3.37.238.221}),
 * 커스텀 도메인({@code yoneodoo.com}, {@code www.yoneodoo.com}).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://3.37.238.221",
                        "https://yoneodoo.com",
                        "https://www.yoneodoo.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}

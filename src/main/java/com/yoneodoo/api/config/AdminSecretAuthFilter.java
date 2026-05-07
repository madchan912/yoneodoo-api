package com.yoneodoo.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 어드민 REST 경로({@code /api/v1/admin/**})에 대한 최소 인증(공유 비밀 헤더) 필터입니다.
 * <p>
 * <b>DB와의 관계</b><br>
 * 필터 자체는 DB를 읽지 않지만, 통과한 요청만 {@link com.yoneodoo.api.admin.AdminService}를 통해
 * 레시피·매핑 데이터를 변경할 수 있게 막는 "관문" 역할을 합니다.
 * <p>
 * CORS 사전 요청({@code OPTIONS})은 브라우저가 헤더를 붙이지 않는 경우가 있어 예외적으로 통과시킵니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminSecretAuthFilter extends OncePerRequestFilter {

    /** 클라이언트가 보내야 하는 HTTP 헤더 이름. */
    public static final String HEADER_NAME = "X-Admin-Secret";
    /** 이 접두사로 시작하는 URI만 이 필터의 검사 대상입니다. */
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    private final AdminProperties adminProperties;

    public AdminSecretAuthFilter(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    /**
     * 어드민 경로가 아니면 필터를 아예 건너뜁니다(일반 API 성능에 영향 없음).
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(ADMIN_PATH_PREFIX);
    }

    /**
     * 어드민 요청마다 시크릿을 검사합니다.
     * <p>
     * 순서: OPTIONS 통과 → 시크릿 미설정 시 503 → 헤더 일치 시 다음 필터/컨트롤러 → 불일치 시 401
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!adminProperties.hasSecret()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Admin secret is not configured");
            return;
        }

        String provided = request.getHeader(HEADER_NAME);
        if (provided != null && provided.equals(adminProperties.secret())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing admin secret");
    }
}

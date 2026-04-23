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
 * {@code /api/v1/admin/**} 요청에 대해 {@code X-Admin-Secret} 헤더를 {@link AdminProperties#secret()}과 비교한다.
 * CORS 사전 요청(OPTIONS)은 헤더 없이 통과시킨다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminSecretAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Admin-Secret";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    private final AdminProperties adminProperties;

    public AdminSecretAuthFilter(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(ADMIN_PATH_PREFIX);
    }

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

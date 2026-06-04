package com.lexai.backend.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个 HTTP 请求绑定一个关联 ID（traceId），写入 SLF4J MDC，使一次请求内多步
 * 调用（AI 网关、合同落库、待办联动等）的日志可被串联追踪，并回写响应头便于前后端对账。
 *
 * <p>沿用调用方传入的 {@code X-Request-Id}（如网关已生成），否则生成短 UUID。
 * 请求结束务必清理 MDC，避免线程复用导致 traceId 串号。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private static String resolveTraceId(String incoming) {
        if (StringUtils.hasText(incoming)) {
            // 防御异常长输入污染日志
            return incoming.length() > 64 ? incoming.substring(0, 64) : incoming;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
